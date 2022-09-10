package osy.kcg.mykotlin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import osy.kcg.mykotlin.databinding.ActivityStatisticBinding
import osy.kcg.utils.RankAdapter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.*


class StatisticActivity : AppCompatActivity() {

    companion object {
        const val TAG = "KakaomapActivity"
    }

    lateinit var binding: ActivityStatisticBinding
    private var dialog : Dialog? = null
    var dialogHandler : Handler = Handler(Looper.getMainLooper())
    var jsHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialWebView()

        binding.statisticAccept.setOnClickListener {
            val pname = binding.statisticPname.selectedItem.toString()
            val url = resources.getString(R.string.serverUrl) + resources.getString(R.string.displayMap) +
                        "?targetName=" + URLEncoder.encode(pname, StandardCharsets.UTF_8.toString())
            dialogHandler.post { dialogControl(true) }
            binding.statisticWebview.loadUrl(url)
        }

        binding.statisticDistrictLabel.setOnClickListener{
            getRank(1)
        }
        binding.statisticDistrictCount.setOnClickListener{
            getRank(1)
        }
        binding.statisticPictureLabel.setOnClickListener{
            getRank(2)
        }
        binding.statisticPictureCount.setOnClickListener{
            getRank(2)
        }
    }

    private var ary : ArrayList<String>? = null
    private fun getRank(index : Int){
        Thread{
            ary = ArrayList()
            val br = BufferedReader(InputStreamReader(URL(resources.getString(R.string.serverUrl) + resources.getString(R.string.getRankUrlJsp)+"?index=$index").openStream()))
            var s = br.readLine() // header
            s = br.readLine()
            while(s != null && s.length>1) {
                ary!!.add(s)
                s = br.readLine()
            }

            dialogHandler.post {
                val alertDialogBuilder = AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat_Light)
                val layout: View = layoutInflater.inflate(R.layout.rank_list_view, null)
                val rankAdapter = RankAdapter(this, ary)
                alertDialogBuilder.setCustomTitle(layout)
                alertDialogBuilder.setAdapter(rankAdapter){ dialogInterface: DialogInterface?, i: Int ->
                    var s = ary!![i].split(",")[0]
                    val url = resources.getString(R.string.serverUrl) + resources.getString(R.string.displayMap) +
                            "?targetName=" + URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
                    dialogHandler.post { dialogControl(true) }
                    binding.statisticWebview.loadUrl(url)
                    dialogInterface!!.dismiss()
                }
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()
                val width = resources.displayMetrics.widthPixels
                val height = resources.displayMetrics.heightPixels
                alertDialog.window!!.setLayout(width * 4/5, height * 3/5)
            }
        }.start()


    }


    private fun dialogControl(toStart : Boolean){
        if(toStart){
            dialog = Dialog(this)
            dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog!!.setContentView(ProgressBar(this))
            dialog!!.setCanceledOnTouchOutside(false)
            dialog!!.show()
        }
        else
            dialog!!.dismiss()
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
                            override fun onPageStarted(s: WebView?, a: String?, d: Bitmap?) {
                            }
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
                }catch(e:Exception){
                }
            }
        }
    }
}