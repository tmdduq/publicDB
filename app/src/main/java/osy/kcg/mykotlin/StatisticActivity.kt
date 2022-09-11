package osy.kcg.mykotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.akndmr.ugly_tooltip.TooltipObject
import osy.kcg.mykotlin.databinding.ActivityStatisticBinding
import osy.kcg.utils.RankAdapter
import osy.kcg.utils.SoundSearcher
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class StatisticActivity : AppCompatActivity() {

    companion object {
        const val TAG = "StatisticActivity"
    }

    lateinit var binding: ActivityStatisticBinding
    private var dialog : Dialog? = null
    var dialogHandler : Handler = Handler(Looper.getMainLooper())
    var jsHandler = Handler(Looper.getMainLooper())
    private val mContext = this
    var fName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialWebView()
        setClickListener()

        var builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val completeFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadCompleteReceiver, completeFilter)
    }

    private fun setClickListener(){
        binding.statisticAccept.setOnClickListener {
            Log.i(TAG, "setClickListener - statisticAccept")
            val pname = binding.statisticPname.text.toString()
            var isPname = false
            resources.getStringArray(R.array.pName).forEach {
                if(it == pname){
                    isPname = true
                    return@forEach
                }
            }
            resources.getStringArray(R.array.sName).forEach {
                if(it == pname){
                    isPname = true
                    return@forEach
                }
            }
            if(!isPname){
                Toast.makeText(this,"소속이 잘못되었습니다.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val url = resources.getString(R.string.serverUrl) + resources.getString(R.string.displayMap) +
                    "?targetName=" + URLEncoder.encode(pname, StandardCharsets.UTF_8.toString())
            dialogHandler.post { dialogControl(true) }
            binding.statisticWebview.loadUrl(url)
        }

        binding.statisticPname.setOnClickListener {
            binding.statisticPname.setText("")
        }
        binding.statisticPname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                binding.statisticDownload.visibility = View.INVISIBLE
                runFirstComplete()

                val string = p0.toString()
                resources.getStringArray(R.array.pName).iterator().forEach {
                    if(it==string) {
                        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(binding.statisticPname.windowToken,0)
                        return@forEach
                    }
                }
                resources.getStringArray(R.array.sName).iterator().forEach {
                    if(it==string) {
                        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(binding.statisticPname.windowToken,0)
                        return@forEach
                    }
                }
            }
        })
        binding.statisticDistrictLabel.setOnClickListener{getRank(1)}
        binding.statisticDistrictCount.setOnClickListener{getRank(1)}
        binding.statisticPictureLabel.setOnClickListener{getRank(2)}
        binding.statisticPictureCount.setOnClickListener{getRank(2)}
        binding.statisticDownload.setOnClickListener {
            val pName = binding.statisticPname.text.toString()
            var isPname = false
            resources.getStringArray(R.array.pName).forEach {
                if(it == pName){
                    isPname = true
                    return@forEach
                }
            }
            if(!isPname || pName=="본청"){
                Toast.makeText(this,"상위부서 다운로드 권한이 없습니다.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Thread{
                try {
                    dialogHandler.post { dialogControl(true) }
                    var urlString = resources.getString(R.string.serverUrl) + resources.getString(R.string.downloadUrlJsp)+"?targetName=$pName"
                    val br = BufferedReader(InputStreamReader(URL(urlString).openStream()))
                    val s = br.readLine()
                    fName = s.split("/")[4]
                    Log.i(TAG, resources.getString(R.string.serverUrl) + s)
                    downloadManager(Uri.parse(resources.getString(R.string.serverUrl) + s))
                }catch(e:Exception){
                    e.printStackTrace()
                    dialogHandler.post { dialogControl(false) }
                }
            }.start()
        }
    }

    private var mDownloadManager: DownloadManager? = null
    private var mDownloadQueueId: Long? = null
    private fun downloadManager(url: Uri) {
        try {
            if (mDownloadManager == null) {
                mDownloadManager =
                    mContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            }
            fName?.let {
                val outputFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/$fName")
                if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs()
                val request = DownloadManager.Request(url)
                request.setTitle("시설물정보를 다운로드합니다.")
                request.setDestinationUri(Uri.fromFile(outputFile))
                request.setAllowedOverMetered(true)
                mDownloadQueueId = mDownloadManager!!.enqueue(request)
            }
        }catch(e:Exception){e.printStackTrace()}
    }


    private var ary : ArrayList<String>? = null
    private fun getRank(index : Int){
        Thread{
            try {
                ary = ArrayList()
                val br = BufferedReader(
                    InputStreamReader(
                        URL(
                            resources.getString(R.string.serverUrl) + resources.getString(R.string.getRankUrlJsp) + "?index=$index"
                        ).openStream()
                    )
                )
                var s = br.readLine() // header
                s = br.readLine()
                while (s != null && s.length > 1) {
                    ary!!.add(s)
                    s = br.readLine()
                }
                dialogHandler.post {
                    val alertDialogBuilder = AlertDialog.Builder(
                        this,
                        androidx.appcompat.R.style.AlertDialog_AppCompat_Light
                    )
                    val layout: View = layoutInflater.inflate(R.layout.rank_list_view, null)
                    val rankAdapter = RankAdapter(this, ary)
                    alertDialogBuilder.setCustomTitle(layout)
                    alertDialogBuilder.setAdapter(rankAdapter) { dialogInterface: DialogInterface?, i: Int ->
                        var s = ary!![i].split(",")[0]
                        val url =resources.getString(R.string.serverUrl) + resources.getString(R.string.displayMap) +"?targetName=" +
                                URLEncoder.encode(s,StandardCharsets.UTF_8.toString())
                        binding.statisticPname.setText(s)
                        dialogHandler.post { dialogControl(true) }
                        binding.statisticWebview.loadUrl(url)
                        dialogInterface!!.dismiss()
                    }
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.show()
                    val width = resources.displayMetrics.widthPixels
                    val height = resources.displayMetrics.heightPixels
                    alertDialog.window!!.setLayout(width * 4 / 5, height * 3 / 5)
                }
            }catch(e: Exception){e.printStackTrace()}
        }.start()
    }


    private fun runFirstComplete(){
        try {
            if (binding.statisticPname.text.isEmpty()) return

            val rstList = java.util.ArrayList<String>()
            for (i in 0 until resources.getStringArray(R.array.sName).size) {
                val pName = resources.getStringArray(R.array.sName)[i]
                val bResult =
                    SoundSearcher().matchString(pName, binding.statisticPname.text.toString())
                if (bResult) rstList.add(pName)
            }
            for (i in 0 until resources.getStringArray(R.array.pName).size) {
                val pName = resources.getStringArray(R.array.pName)[i]
                val bResult =
                    SoundSearcher().matchString(pName, binding.statisticPname.text.toString())
                if (bResult) rstList.add(pName)
            }

            if (rstList.size > 0) {
                val rstItem = arrayOfNulls<String>(rstList.size)
                rstList.toArray(rstItem)
                binding.statisticPname.setAdapter(ArrayAdapter<String>(this,R.layout.spinner_item_dropdown,rstItem))
                binding.statisticPname.showDropDown()
                rstList.forEach { Log.i(TAG, "$it") }
                return
            }
            else{
                binding.statisticPname.dismissDropDown()
                return
            }
        }catch(e:Exception){}
    }

    private fun dialogControl(toStart : Boolean){
        try {
            if (toStart) {
                dialog = Dialog(this)
                dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog!!.setContentView(ProgressBar(this))
                dialog!!.setCanceledOnTouchOutside(false)
                dialog!!.show()
            } else
                dialog!!.dismiss()
        }catch(e:Exception){
            e.printStackTrace()
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initialWebView(){
        binding.statisticWebview.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(s: WebView?, a: String?, d: Bitmap?) {
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    dialogHandler.post { dialogControl(false) }
                }
            }
            webChromeClient = object : WebChromeClient(){
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                    val newWebView = WebView(this@StatisticActivity).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(s: WebView?, a: String?, d: Bitmap?) {}
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                dialogHandler.post { dialogControl(false) }
                            }
                        }
                        settings.javaScriptEnabled = true
                    }
                    val dialog = Dialog(this@StatisticActivity).apply {
                        setContentView(newWebView)
                        window!!.attributes.width = ViewGroup.LayoutParams.MATCH_PARENT
                        window!!.attributes.height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    dialog.show()
                    newWebView.webChromeClient = object : WebChromeClient(){
                        override fun onCloseWindow(window: WebView?) {
                            dialog.dismiss()
                        }
                    }
                    (resultMsg?.obj as WebView.WebViewTransport).webView = newWebView
                    resultMsg.sendToTarget()
                    return true
                }
            }
        }
        binding.statisticWebview.settings.javaScriptEnabled = true
        binding.statisticWebview.addJavascriptInterface(AndroidBridge() , "sendToNative")
    }

    override fun onBackPressed() {
        when {
            binding.statisticWebview.url.isNullOrEmpty() -> super.onBackPressed()
            binding.statisticWebview.url!!.contains("DATA") -> binding.statisticWebview.goBack()
            else -> super.onBackPressed()
        }
    }

    inner class AndroidBridge{
        @JavascriptInterface
        fun sendData(msg: String){
            jsHandler.post {
                var message = msg
                var s1 : String
                var s2 : String
                try{
                    s1 = message.split(",")[0]+"건"
                    s2 = message.split(",")[1]+"건"
                }catch(e:Exception){
                    s1 = "0건"
                    s2 = "0건"
                }
                try{
                    binding.statisticPictureCount.text = s1
                    binding.statisticDistrictCount.text = s2
                    Log.i(TAG, "JavaScript : $s1 , $s2");
                }catch(e:Exception){
                }
                if(s1=="0건") binding.statisticDownload.visibility = View.INVISIBLE
                else binding.statisticDownload.visibility = View.VISIBLE
            }
        }
    }

    private val downloadCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            dialogHandler.post { dialogControl(false) }
            val reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (mDownloadQueueId == reference) {
                val query = DownloadManager.Query() // 다운로드 항목 조회에 필요한 정보 포함
                query.setFilterById(reference)
                val cursor: Cursor = mDownloadManager!!.query(query)
                cursor.moveToFirst()
                val columnIndex: Int = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val columnReason: Int = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val status: Int = cursor.getInt(columnIndex)
                val reason: Int = cursor.getInt(columnReason)
                cursor.close()
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    AlertDialog.Builder(context)
                    .setMessage("다운로드가 완료되었습니다.")
                        .setPositiveButton("확인"){ dialog, _ ->
                            dialog.cancel();
                            var intent = Intent();
                            intent.action = Intent.ACTION_VIEW;
                            val outputFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/$fName")
                            intent.setDataAndType(Uri.fromFile(outputFile), "application/vnd.ms-excel");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        }.show()
                }
            }
        }
    }
}