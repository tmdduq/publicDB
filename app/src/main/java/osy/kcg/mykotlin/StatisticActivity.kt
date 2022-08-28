package osy.kcg.mykotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import osy.kcg.mykotlin.databinding.ActivityStatisticBinding
import osy.kcg.utils.SoundSearcher
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.ArrayList

class StatisticActivity : AppCompatActivity() {

    companion object {
        const val TAG = "KakaomapActivity"
    }

    lateinit var binding: ActivityStatisticBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialWebView()

        binding.statisticAccept.setOnClickListener{
            val pname = binding.statisticPname.text.toString()
            val url = resources.getString(R.string.serverUrl) + resources.getString(R.string.displayMap) +
                    "?targetName="+ URLEncoder.encode(pname,StandardCharsets.UTF_8.toString())
            binding.statisticWebview.loadUrl(url)
        }

        binding.statisticPname.setOnKeyListener{ v: View?, code:Int?, _:Any?->
            if(code == KeyEvent.KEYCODE_ENTER){
                (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        binding.statisticPname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun afterTextChanged(p0: Editable?) {
                runFirstComplete()
                val string = binding.statisticPname.text.toString()
                resources.getStringArray(R.array.pName).iterator().forEach {
                    if(it==string) {
                        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(binding.statisticPname.windowToken,0)
                        return@forEach
                    }
                }
            }
        })
    }
    private fun runFirstComplete(){
        if(binding.statisticPname.text.isEmpty()) return
        val rstList = ArrayList<String>()
        for( i in 0 until resources.getStringArray(R.array.pName).size){
            val pName = resources.getStringArray(R.array.pName)[i]
            val bResult = SoundSearcher().matchString(pName, binding.statisticPname.text.toString())
            if(bResult) rstList.add(pName)
        }
        if(rstList.size>0){
            val rstItem = arrayOfNulls<String>(rstList.size)
            rstList.toArray(rstItem)
            binding.statisticPname.setAdapter(ArrayAdapter<String>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, rstItem))
            binding.statisticPname.showDropDown()
            return
        }
        binding.statisticPname.dismissDropDown()
        return
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initialWebView(){
        binding.statisticWebview.apply {
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient(){
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                    val newWebView = WebView(this@StatisticActivity).apply {
                        webViewClient = WebViewClient()
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
    var handler = Handler(Looper.getMainLooper())
    inner class AndroidBridge{
        @JavascriptInterface
        fun sendData(msg: String){
            handler.post {
                var message = msg
                var s1 : String
                var s2 : String
                try{
                    s1 = message.split(",")[0]
                    s2 = message.split(",")[1]
                }catch(e:Exception){
                    s1 = "0"
                    s2 = "0"
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