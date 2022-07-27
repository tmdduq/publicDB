package osy.kcg.mykotlin;

import android.icu.text.Edits;
import android.util.Log;

import com.kcg.facillitykotlin.RV;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;

import kotlin.jvm.internal.CollectionToArray;

public class HTTP {
    final String TAG = "HTTP";
    RV param;

    public HTTP(RV param){
        this.param = param;
    }

    public int DoFileUpload(String absolutePath) {
        String apiUrl = param.getServerUrl() + param.getImageUploadUrlJsp();
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

                if (conn.getResponseCode() == 200) {
                    InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuffer stringBuffer = new StringBuffer();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line);
                    }
                }
                else return 3;
                mFileInputStream.close();
                dos.close();
                TRANSFER_RESULT = 1;
                return TRANSFER_RESULT;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return TRANSFER_RESULT;
        }
        return TRANSFER_RESULT;
    }

    public int DoValuesUpload(String activityName){
        StringBuilder dataSet = new StringBuilder();
        String url;
        if(activityName.contains("point")) {
            dataSet.append("?upTime=" + URLEncoder.encode(param.getKa_upTime()));
            dataSet.append("&name=" + URLEncoder.encode(param.getKa_name()));
            dataSet.append("&pname=" + URLEncoder.encode(param.getKa_pname()));
            dataSet.append("&type=" + URLEncoder.encode(param.getKa_type()));
            dataSet.append("&point=" + URLEncoder.encode(param.getKa_point()));
            url = param.getServerUrl() + param.getSavePointUrlJsp() + dataSet.toString();
        }
        else{
            dataSet.append("?timeStamp=" + URLEncoder.encode(param.getTimeStamp()));
            dataSet.append("&add=" + URLEncoder.encode(param.getAddress()));
            dataSet.append("&latitude=" + URLEncoder.encode(param.getLatitude()));
            dataSet.append("&longitude=" + URLEncoder.encode(param.getLongitude()));
            dataSet.append("&form1=" + URLEncoder.encode(param.getFm2_rndur()));
            dataSet.append("&form2=" + URLEncoder.encode(param.getFm3_wkdth()));
            dataSet.append("&form3=" + URLEncoder.encode(param.getFm1_tnsckfwkdth()));
            dataSet.append("&form4=" + URLEncoder.encode(param.getFm4_tltjf()));
            dataSet.append("&form4_check=" + URLEncoder.encode(param.getFm4_tltjf_check()));
            dataSet.append("&phoneNo=" + URLEncoder.encode(param.getPhoneNo()));
            dataSet.append("&phoneName=" + URLEncoder.encode(param.getFm1_tnsckfwkdth_auto()));
            dataSet.append("&imageName=" + URLEncoder.encode(param.getImageName()));
            url = param.getServerUrl() + param.getSaveUrlJsp() + dataSet.toString();
        }


        Log.d(TAG, "DoValuesUpload() - URL : "+ url);
        return HttpValuesUpload(url);
    }

    public String VersionCheck(String serverUrl, String versionUrl){
        try {
            URL connectUrl = new URL(serverUrl + versionUrl);
            URLConnection urlConnection = connectUrl.openConnection();
            InputStreamReader ir = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(ir);
            return br.readLine();
        } catch (Exception e) {
            Log.d(TAG, "VersionCheck - "+e);
            e.printStackTrace();
        }
        return null;
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
