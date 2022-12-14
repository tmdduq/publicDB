package osy.kcg.mykotlin

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import osy.kcg.mykotlin.databinding.ActivityLogoBinding

class LogoActivity : AppCompatActivity() {
    private var TAG = "LogoActivity"
    private var dialog : Dialog? = null

    private lateinit var binding: ActivityLogoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        binding = ActivityLogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        attachListener()
        checkVersion()
    }

    private fun dialogControl(YN : Boolean){
        if(YN){
            dialog = Dialog(this)
            dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog!!.setContentView(ProgressBar(this))
            dialog!!.setCanceledOnTouchOutside(false)
            dialog!!.show()
        }
        else
            dialog!!.dismiss()
    }

    private fun checkVersion() {
        Log.i(TAG, "checkVersion -> start")
        var serverVersion = 9999
        dialogControl(true)
        Thread{
            Log.i(TAG, "checkVersion Thread -> start")
            try {
                serverVersion = HTTP(null, resources.getString(R.string.serverUrl))
                    .VersionCheck(resources.getString(R.string.versionUrl))
                Log.i(TAG, "checkVersion : $serverVersion")
            }catch (e:Exception){ serverVersion = -1}

            val mContext = this
            object : Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    dialogControl(false)
                    var thisVersion = BuildConfig.VERSION_CODE
                    if(serverVersion == -1){
                        AlertDialog.Builder(mContext).setTitle("?????? ?????? ??????").setMessage("????????? ????????? ??? ????????????.\n??????????????? ?????????????????????????")
                            .setCancelable(false)
                            .setPositiveButton("??????") { _: DialogInterface, _: Int ->
                                object : Handler(Looper.getMainLooper()){
                                    override fun handleMessage(msg: Message) {
                                        super.handleMessage(msg)
                                        try {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                                        } catch (anfe: ActivityNotFoundException) {
                                            startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))}
                                        finish()
                                    }
                                }.sendEmptyMessage(1)
                            }
                            .setNegativeButton("?????????"){ _: DialogInterface, _: Int -> finish()  }
                            .create().show()
                    }
                    else if((serverVersion != thisVersion) && ( (serverVersion/10) - (BuildConfig.VERSION_CODE/10) ) >0){
                        AlertDialog.Builder(mContext).setTitle("???????????? ??????").setMessage("????????? ??????????????? ???????????????.\n??????????????? ????????????.")
                            .setCancelable(false)
                            .setPositiveButton("??????") { _: DialogInterface, _: Int ->
                                object : Handler(Looper.getMainLooper()){
                                    override fun handleMessage(msg: Message) {
                                        super.handleMessage(msg)
                                        try {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                                        } catch (anfe: ActivityNotFoundException) {
                                            startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))}
                                        finish()
                                    }
                                }.sendEmptyMessage(1)
                            }
                            .setNegativeButton("?????????"){ _: DialogInterface, _: Int -> finish()  }
                            .create().show()
                    }
                    else if(serverVersion > thisVersion){
                        AlertDialog.Builder(mContext).setTitle("???????????? ??????").setMessage("?????? ????????? ?????????????????????.\n???????????????????????????????")
                            .setPositiveButton("??????") { _: DialogInterface, _: Int ->
                                object : Handler(Looper.getMainLooper()){
                                    override fun handleMessage(msg: Message) {
                                        super.handleMessage(msg)
                                        try {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                                        } catch (anfe: ActivityNotFoundException) {
                                            startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))}
                                    }
                                }.sendEmptyMessage(1)
                            }
                            .setNegativeButton("?????????"){ dialog: DialogInterface, _: Int -> dialog.dismiss()  }
                            .create().show()
                    }
                }
            }.sendEmptyMessageDelayed(0,1000)
        }.start()

    }



    private fun attachListener(){
        binding.logoLoginButton.setOnClickListener{
            startActivity(intent)

            when( binding.logoCodeEditText.text.toString().uppercase() ){
                "" -> Toast.makeText(this, "????????? ???????????????.", Toast.LENGTH_SHORT).show()
                resources.getString(R.string.loginCode) -> {
                    val intent = Intent(this, LobbyActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout)
                    finish()
                }
                else -> Toast.makeText(this, "????????? ???????????????.", Toast.LENGTH_SHORT).show()
            }
            return@setOnClickListener
        }

        binding.logoCodeEditText.setOnKeyListener{ v: View?, code: Int?, _: Any? ->
            if(code == KeyEvent.KEYCODE_ENTER){
                val imm : InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false

        }
    }

}