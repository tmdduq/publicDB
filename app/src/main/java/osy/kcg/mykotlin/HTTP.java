package osy.kcg.mykotlin;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HTTP {
    final String TAG = "HTTP";
    Map<Integer,String> param ;
    String serverUrl;

    public HTTP(Map<Integer,String> param, String serverUrl){
        this.param = param;
        this.serverUrl = serverUrl;
    }

    public int DoFileUpload(String absolutePath, String imageUploadUrlJsp) {
        String apiUrl = serverUrl + imageUploadUrlJsp;
        return HttpFileUpload(apiUrl, "", absolutePath);
    }

    public int HttpFileUpload(String urlString, String params, String fileName) {
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int TRANSFER_RESULT = 0;

        try {
            File sourceFile = new File(fileName);
            DataOutputStream dos;

            if (!sourceFile.isFile()) {
                Log.e(TAG, "HttpFileUpload - Source File not exist :" + fileName);
            } else {

                FileInputStream mFileInputStream = new FileInputStream(sourceFile);

                URL connectUrl = new URL(urlString);

                // open connection
                HttpURLConnection conn = (HttpURLConnection) connectUrl.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);
                conn.setConnectTimeout(5000);
                // write data
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                int bytesAvailable = mFileInputStream.available();
                int maxBufferSize = 1024 * 1024;
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);

                byte[] buffer = new byte[bufferSize];
                int bytesRead = mFileInputStream.read(buffer, 0, bufferSize);

                // read image
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = mFileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = mFileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                mFileInputStream.close();
                dos.flush(); // finish upload...


                if (conn.getResponseCode() != 200) return 3;

                InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(tmp);
                String result = "0";
                String line;
                while ((line = reader.readLine()) != null)
                    result = line;

                mFileInputStream.close();
                dos.close();
                return Integer.parseInt(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return 0;
    }

    public int DoValuesUpload(String activityName, String jspUrl) throws UnsupportedEncodingException {
        StringBuilder dataSet = new StringBuilder();
        String url;
        if(activityName.contains("point")) {
            dataSet.append("?upTime=").append(URLEncoder.encode(param.get(KakaomapActivity.upTime), StandardCharsets.UTF_8.toString()));
            dataSet.append("&name=").append(URLEncoder.encode(param.get(KakaomapActivity.name), StandardCharsets.UTF_8.toString()));
            dataSet.append("&pname=").append(URLEncoder.encode(param.get(KakaomapActivity.pName), StandardCharsets.UTF_8.toString()));
            dataSet.append("&type=").append(URLEncoder.encode(param.get(KakaomapActivity.type), StandardCharsets.UTF_8.toString()));
            dataSet.append("&placeType=").append(URLEncoder.encode(param.get(KakaomapActivity.type), StandardCharsets.UTF_8.toString()));
            dataSet.append("&address=").append(URLEncoder.encode(param.get(KakaomapActivity.address), StandardCharsets.UTF_8.toString()));
            dataSet.append("&point=").append(URLEncoder.encode(param.get(KakaomapActivity.point), StandardCharsets.UTF_8.toString()));
        }
        else{
            dataSet.append("?timeStamp=").append(URLEncoder.encode(param.get(MainActivity.timeStamp),StandardCharsets.UTF_8.toString()));
            dataSet.append("&add=").append(URLEncoder.encode(param.get(MainActivity.address), StandardCharsets.UTF_8.toString()));
            dataSet.append("&latitude=").append(URLEncoder.encode(param.get(MainActivity.latitude), StandardCharsets.UTF_8.toString()));
            dataSet.append("&longitude=").append(URLEncoder.encode(param.get(MainActivity.longitude), StandardCharsets.UTF_8.toString()));
            dataSet.append("&form1=").append(URLEncoder.encode(param.get(MainActivity.districtType), StandardCharsets.UTF_8.toString()));
            dataSet.append("&form2=").append(URLEncoder.encode(param.get(MainActivity.placeType), StandardCharsets.UTF_8.toString()));
            dataSet.append("&form3=").append(URLEncoder.encode(param.get(MainActivity.placeExplain), StandardCharsets.UTF_8.toString()));
            dataSet.append("&form4=").append(URLEncoder.encode(param.get(MainActivity.facilityType), StandardCharsets.UTF_8.toString()));
            dataSet.append("&form4_check=").append(URLEncoder.encode(param.get(MainActivity.mainManager), StandardCharsets.UTF_8.toString()));
            dataSet.append("&phoneNo=").append(URLEncoder.encode(param.get(MainActivity.phoneNo), StandardCharsets.UTF_8.toString()));
            dataSet.append("&phoneName=").append(URLEncoder.encode(param.get(MainActivity.pName), StandardCharsets.UTF_8.toString()));
            dataSet.append("&imageName=").append(URLEncoder.encode(param.get(MainActivity.imageName), StandardCharsets.UTF_8.toString()));
        }
        url = serverUrl + jspUrl + dataSet;


        Log.d(TAG, "DoValuesUpload() - URL : "+ url);
        return HttpValuesUpload(url);
    }

    public int VersionCheck(String versionUrl){
        try {
            URL connectUrl = new URL(serverUrl + versionUrl);
            URLConnection urlConnection = connectUrl.openConnection();
            urlConnection.setConnectTimeout(3000);
            InputStreamReader ir = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(ir);
            return Integer.parseInt(br.readLine());
        } catch (Exception e) {
            Log.d(TAG, "VersionCheck - "+e);
            e.printStackTrace();
            return -1;
        }
    }

    public int HttpValuesUpload(String url){
        try {
            URL connectUrl = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) connectUrl.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
            urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            urlConnection.setRequestProperty("Accept","*/*");
            urlConnection.setConnectTimeout(5000);
            urlConnection.connect();

            int error = urlConnection.getResponseCode();
            Log.d(TAG, "HttpValuesUpload() - HTTP Response Code : "+error);

            InputStreamReader ir = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader in = new BufferedReader(ir);

            String flag = in.readLine();

            Log.d(TAG, "HttpValuesUpload() - server result flag : "+flag);
            return Integer.parseInt(flag);

        } catch (IOException e) {
            Log.d(TAG, "HttpValuesUpload() - Exception : "+e);
            e.printStackTrace();
            return 0;
        } catch (Exception e) {
            Log.d(TAG, "HttpValuesUpload() - Exception : "+e);
            e.printStackTrace();
            return 0;
        }
    }

    public String[] Coord2Address(String lat, String lon, String key, String url){
        Log.d(TAG, "Coord2Address() - start : ");
        try{
            URL connectUrl = new URL(url + "x=" + lon +"&y="+lat + "&input_coord=WGS84");
            HttpURLConnection urlConnection = (HttpURLConnection) connectUrl.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
            urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            urlConnection.setRequestProperty("Authorization","KakaoAK "+key);
            urlConnection.setRequestProperty("Accept","*/*");
            urlConnection.setConnectTimeout(5000);
            urlConnection.connect();


            int error = urlConnection.getResponseCode();
            Log.d(TAG, "Coord2Address() - HTTP Response Code : "+error);

            InputStreamReader ir = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader in = new BufferedReader(ir);
            String s = in.readLine();
            Log.d(TAG, "Coord2Address() json : "+s);
            String road_address;
            String address;
            JSONArray jsonArray = new JSONObject(s).getJSONArray("documents");

            try{
                road_address = jsonArray.getJSONObject(0).getJSONObject("road_address").getString("address_name");
//                Log.d(TAG,"도로명 : " + road_address);
            }catch(Exception e){
                road_address = null;
            }
            try{
                address = jsonArray.getJSONObject(0).getJSONObject("address").getString("address_name");
//                Log.d(TAG,"지번 : " + address);
            }catch(Exception e){
                address = null;
            }
            return new String[]{road_address, address} ;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
