package osy.kcg.mykotlin

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.magicgoop.tagsphere.OnTagTapListener
import com.magicgoop.tagsphere.item.TagItem
import osy.kcg.mykotlin.databinding.ActivityLobbyBinding
import osy.kcg.utils.VectorDrawableTagItem

class LobbyActivity : AppCompatActivity(){

    val drawableResList = listOf(
        R.drawable.a1, R.drawable.a2, R.drawable.a3, R.drawable.a4, R.drawable.a5, R.drawable.a6,
        R.drawable.a7, R.drawable.a8, R.drawable.a9, R.drawable.a10, R.drawable.a11, R.drawable.a11,
        R.drawable.a12, R.drawable.a13, R.drawable.a14, R.drawable.a15, R.drawable.a16, R.drawable.a17, R.drawable.a18,
        R.drawable.a19, R.drawable.a20, R.drawable.a21, R.drawable.a22, R.drawable.a23, R.drawable.a24, R.drawable.a25,
        R.drawable.a26, R.drawable.a27
    )

    private lateinit var binding: ActivityLobbyBinding
    private fun getVectorDrawable(id: Int): Drawable? = ContextCompat.getDrawable(this, id)
    var tryCount : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        binding = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        attachClickListener()
        initTagView()
    }

    private fun attachClickListener(){
        binding.lobbyFirstButton.setOnClickListener{
            if(!requestEachPermission("?????? ????????? ?????? [????????????1]", Manifest.permission.ACCESS_FINE_LOCATION)) return@setOnClickListener
            if(!requestEachPermission("?????? ????????? ?????? [????????????2]", Manifest.permission.ACCESS_COARSE_LOCATION)) return@setOnClickListener
            if(!checkLocationServicesStatus()){
                showDialogForLocationServiceSetting()
                return@setOnClickListener
            }
            startActivity(Intent(this, Kakaomap2Activity::class.java))
            overridePendingTransition(R.anim.fadein, R.anim.fadeout)
        }
        binding.lobbySecondButton.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.fadein, R.anim.fadeout)
        }
        binding.lobbyThirdButton.setOnClickListener{
            startActivity(Intent(this, FallCarActivity::class.java))
            overridePendingTransition(R.anim.fadein, R.anim.fadeout)
        }
        binding.lobbyFourButton.setOnClickListener{
            startActivity(Intent(this, StatisticActivity::class.java))
            overridePendingTransition(R.anim.fadein, R.anim.fadeout)
        }

    }

    private fun initTagView() {
        val tags = mutableListOf<VectorDrawableTagItem>()
        drawableResList.forEach { id ->
            getVectorDrawable(id)?.let {
                tags.add(VectorDrawableTagItem(it))
            }
        }
        binding.lobbyTagSphereView.addTagList(tags)
        binding.lobbyTagSphereView.setRadius(1f)
        binding.lobbyTagSphereView.setOnTagTapListener(object : OnTagTapListener {
            override fun onTap(tagItem: TagItem) {
                for( t in 0 until tags.size)
                    if(tagItem.compareTo(tags[t]) == 0) binding.lobbyFooter.setBackgroundResource(
                        drawableResList[t])
//                tagItem.compareTo(tags[0]) == 0 -> s = "???"
//                Toast.makeText(requireContext(), "Stay calm and don't get sick $s", Toast.LENGTH_SHORT).show()
            }
        })
        binding.lobbyTagSphereView.startAutoRotation()
        setColorText(binding.lobbyFirstText, "????????????", "#0b4094")
        setColorText(binding.lobbyFirstText, "??????", "#0b4094")
        setColorText(binding.lobbySecondText, "?????????????????????", "#0b4094")
        setColorText(binding.lobbySecondText, "??????", "#0b4094")
        setColorText(binding.lobbySecondText, "??????", "#0b4094")
        setColorText(binding.lobbyThirdText, "????????? ????????? ??????", "#0b4094")
        setColorText(binding.lobbyThirdText, "?????????", "#0b4094")

    }

    private fun setColorText(view : TextView, word : String, color : String){
        val spannableString = SpannableString(view.text)
        var start: Int = view.text.indexOf(word)
        var end = start + word.length
        if(start<0) return
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor(color)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        spannableString.setSpan(RelativeSizeSpan(1.3f), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.text = spannableString
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var checkPermission: Boolean = true
        for (i: Int in grantResults.indices) {
            Log.i(MainActivity.TAG, " ${permissions[i]} -> ${if (grantResults[i] == PackageManager.PERMISSION_GRANTED) "ok" else "fail"}")
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                checkPermission = false
        }
        if(!checkPermission) {
            var sb = Snackbar.make(binding.lobbyFooter,"????????? ????????? ????????????. ????????? ???????????????.",Snackbar.LENGTH_SHORT)
            if(tryCount++ > 1)
                sb.setAction("????????????") {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                    tryCount = 0
                }
            sb.show()
        }
    }

    private fun requestEachPermission(name : String, permissionString : String) : Boolean{
        if(ActivityCompat.checkSelfPermission(this, permissionString) != PackageManager.PERMISSION_GRANTED){
            Log.i(MainActivity.TAG,"requestEachPermission - $permissionString -> fail")
            Snackbar.make(binding.lobbyFooter, "$name ????????? ???????????????.", Snackbar.LENGTH_INDEFINITE)
                .setAction("????????????") {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,permissionString)){
                        Snackbar.make(binding.lobbyFooter, "???????????? ????????? ????????????.\n????????? ???????????? ?????? ??????????????????.", Snackbar.LENGTH_INDEFINITE)
                            .setAction("??????"){
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = Uri.fromParts("package", packageName, null)
                                startActivity(intent)
                            }.show()
                    }
                    else {
                        ActivityCompat.requestPermissions(this, arrayOf(permissionString), 0)
                    }
                }.show()
        }
        else
            return true
        return false
    }

    private fun checkLocationServicesStatus() : Boolean{
        Log.i(MainActivity.TAG, "checkLocationServicesStatus()")
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }
    private fun showDialogForLocationServiceSetting(){
        Log.i(MainActivity.TAG,"showDialogForLocationServiceSetting()")
        val builder = AlertDialog.Builder(this@LobbyActivity)
        builder.setTitle("?????? ????????????")
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n?????? ????????? ?????????????????????????")
        builder.setCancelable(true)
        builder.setPositiveButton("???????????? ??????"){_:DialogInterface, _:Int->
            val intent = Intent()
            intent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
            startActivity(intent)
        }
        builder.setNegativeButton("?????????"){ dia:DialogInterface, _:Int->
            dia.cancel()
            Toast.makeText(this, "?????? ???????????? ????????? ?????????????????? ?????????", Toast.LENGTH_SHORT).show()
        }
        builder.setCancelable(false)
        builder.create().show()
    }


}