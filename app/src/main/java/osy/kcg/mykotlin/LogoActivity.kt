package osy.kcg.mykotlin

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kcg.facillitykotlin.RV
import osy.kcg.mykotlin.databinding.ActivityLogoBinding

class LogoActivity : AppCompatActivity() {
    private var TAG = "LogoActivity"
    private lateinit var binding: ActivityLogoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        binding = ActivityLogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        attachListener()
    }

    override fun onStart() {
        super.onStart()
        checkVersion()
    }

    private fun checkVersion() {
        Log.i(TAG, "checkVersion -> start")
        var version = 0
        var verString: String
        Thread{
            Log.i(TAG, "checkVersion Thread -> start")
            try {
                verString = HTTP(RV()).VersionCheck(resources.getString(R.string.serverUrl), resources.getString(R.string.versionUrl))
                Log.i(TAG, "checkVersion : $verString")
                version = try{
                    Integer.parseInt(verString)
                }catch(e: Exception){
                    0
                }
            }catch (e:java.lang.Exception){ }


            val mContext = this
            object : Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    if(version != BuildConfig.VERSION_CODE){
                        AlertDialog.Builder(mContext).setTitle("업데이트 권고").setMessage("신규 버전이 출시되었습니다.\n업데이트하시겠습니까?")
                            .setPositiveButton("확인") { _: DialogInterface, _: Int ->
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
                            .setNegativeButton("아니오"){ dialog: DialogInterface, _: Int -> dialog.dismiss()  }
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
                "" -> Toast.makeText(this, "코드를 입력하세요.", Toast.LENGTH_SHORT).show()
                resources.getString(R.string.loginCode) -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else -> Toast.makeText(this, "코드가 틀렸습니다.", Toast.LENGTH_SHORT).show()
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