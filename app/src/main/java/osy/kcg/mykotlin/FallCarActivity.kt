package osy.kcg.mykotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import io.akndmr.ugly_tooltip.TooltipBuilder
import io.akndmr.ugly_tooltip.TooltipContentPosition
import io.akndmr.ugly_tooltip.TooltipDialog
import io.akndmr.ugly_tooltip.TooltipObject
import osy.kcg.mykotlin.databinding.ActivityFallcarBinding
import osy.kcg.utils.SoundSearcher
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


open class FallCarActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TAG = "MainActivity";
        const val timeStamp = 1
        const val address = 2
        const val latitude =3
        const val longitude =4
        const val pointExplain = 5
        const val fm1_tnsckfwkdth_auto = 6
        const val districtType = 7
        const val placeType = 8
        const val pointType = 9
        const val mainManager = 10
        const val phoneNo = 11
        const val phoneName = 12
        const val imageName = 13
    }
    private var mContext : Context? = null
    var logTextView : EditText? = null

    var param : HashMap<Int, String> = hashMapOf(
        timeStamp to "", address to "", latitude to "", longitude to "", pointExplain to "", fm1_tnsckfwkdth_auto to "",
        districtType to "", placeType to "", pointType to "", phoneNo to "", phoneName to "", imageName to "",
        mainManager to ""
    )

    var isRunningThread = false
    private var imageFilepath : String? = null
    var tryCount : Int = 0
    var tooltipDialog : TooltipDialog? = null

    lateinit var binding : ActivityFallcarBinding

    protected fun log(TAG: String, log: String){
        try {
            object : Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    Log.i(TAG, log)
                    logTextView!!.setText("$log \n ${logTextView!!.text}")
                }

            }.sendEmptyMessage(0)

        }catch(e : Exception){
            e.printStackTrace()}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        binding = ActivityFallcarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        log(TAG,"onCreate")
        mContext = this
        logTextView = binding.fallcarLogView

        attachClickListner()
        requestPermission()
        initTooltipDialog()
        if(!checkLocationServicesStatus()) showDialogForLocationServiceSetting()

        registerReceiver(NotificationReceiver(), IntentFilter("osy.kcg.myKotlin"))

    }

    private fun initTooltipDialog(){
        tooltipDialog = TooltipBuilder()
            .setPackageName(packageName)
            .titleTextColorRes(R.color.teal_200)  //title textcolor
            .textColorRes(R.color.tooltip_textColor)       //contents textcolor
            .shadowColorRes(R.color.tooltip_backgroundColor) // background fadeout color
            .titleTextSizeRes(R.dimen.spacing_normal)
            .textSizeRes(R.dimen.text_normal)
            .backgroundContentColorRes(R.color.tooltip_box_backgroundColor) // Tooltip background color
            .setTooltipRadius(io.akndmr.ugly_tooltip.R.dimen.tooltip_radius) //Optional tooltip corder radius
            .useCircleIndicator(true)
            .clickable(true) // Click anywhere to continue
            .setFragmentManager(this.supportFragmentManager)
            .showSpotlight(true) //Optional spotlight
            .showBottomContainer(false)
//            .useArrow(true) // Optional tooltip pointing arrow
//            .finishString()
//            .finishString(finishStringText = "시작하기") // Optional finishStringRes or finishStringText
//            .circleIndicatorBackgroundDrawableRes(io.akndmr.ugly_tooltip.R.drawable.selector_circle)
//            .shouldShowIcons(true) // Optional tooltip next/prev icons
//            .prevString(io.akndmr.ugly_tooltip.R.string.previous)
//            .useSkipWord(useSkipWord = true) // Optional tooltip skip option
//            .lineColorRes(io.akndmr.ugly_tooltip.R.color.line_color) // Optional tooltip button seperator line color
//            .lineWidthRes(io.akndmr.ugly_tooltip.R.dimen.line_width) // Optional tooltip button seperator line width
            .build()
    }
    private fun tooltipDialogContents(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            binding.fallcarHelp.setOnClickListener {
                val tooltips: ArrayList<TooltipObject> = ArrayList()
                tooltips.add(//C:\Users\Osy\AndroidStudioProjects\myKotlin\app\src\main\res\mipmap-hdpi\xpng.png
                    TooltipObject(
                        view = binding.fallcarTakePic,
                        title = "① 사진촬영",
                        text = "등록하고 싶은 <font color=\"#FFC300\">시설물</font>을 카메라로 <font color=\"#FFC300\">촬영</font>해주세요.",
                        tooltipContentPosition = TooltipContentPosition.TOP,
                        tintBackgroundColor = ResourcesCompat.getColor(resources, R.color.purple_700, null)
                    )
                )
                tooltips.add(
                    TooltipObject(
                        binding.imageView,
                        "② 사진확인","<font color=\"#FFC300\">촬영한 사진</font>이 잘 나왔는지, 흔들리지는 않았는지 <font color=\"#FFC300\">확인</font>해주세요.",
                        TooltipContentPosition.BOTTOM))
                tooltips.add(TooltipObject(
                    binding.fallcarContents,
                    "③ 부가정보 입력",
                    "등록한 사진의 <font color=\"#FFC300\">정보</font>를 <font color=\"#FFC300\">채워해야요.</font> ",
                    TooltipContentPosition.TOP))
                tooltips.add(TooltipObject(
                    binding.fallcarPnameValue,
                    "③₁ 소속",
                    "당신이 <font color=\"#FFC300\">어디 소속</font>인지 적어주세요.",
                    TooltipContentPosition.BOTTOM))
                tooltips.add(TooltipObject(
                    binding.fallcarPointExplainValue,
                    "③₂장소설명",
                    "사진의 장소가 주소만으로는 이해하기 어려워요. <font color=\"#FFC300\">주소엔 표시되지 않는 위치</font>를 상세하게 입력해주세요.",
                    TooltipContentPosition.BOTTOM))
                tooltips.add(TooltipObject(
                    binding.fallcarPlaceTypeValue,
                    "③₃장소분류",
                    "이 장소는 <font color=\"#FFC300\">어떤 장소로 분류</font>되는지 선택해 주세요.",
                    TooltipContentPosition.TOP))
                tooltips.add(TooltipObject(
                    binding.fallcarPointType,
                    "③₄어떤 걸 촬영하셨나요?",
                    "차량이 <font color=\"#FFC300\">추락한 위치</font>인가요? 아니면 추락한 곳 근처에 있는 <font color=\"#FFC300\">표지판</font> 인가요?",
                    TooltipContentPosition.TOP))
                tooltips.add(TooltipObject(
                    binding.fallcarTransfer,"마지막!",
                    "<font color=\"#FFC300\">전송하기</font> 버튼을 눌러서 작성하신 <font color=\"#FFC300\">안전정보</font>를 <font color=\"#FFC300\">국민</font>에게 <font color=\"#FFC300\">제공</font>해주세요.",
                    TooltipContentPosition.TOP
                )
                )
                tooltipDialog?.show(this, supportFragmentManager, "SHOWCASE_TAG", tooltips)
            }
        }
    }



    @SuppressLint("NewApi")
    private fun attachClickListner(){
        binding.fallcarTakePic.setOnClickListener(this)
        binding.fallcarLongitudeLabel.setOnClickListener(this)
        binding.fallcarLongitudeValue.setOnClickListener(this)
        binding.fallcarLatitudeLabel.setOnClickListener(this)
        binding.fallcarLatitudeValue.setOnClickListener(this)
        binding.fallcarPointExplainValue.setOnClickListener(this)
        binding.fallcarDistrictTypeValue.setOnClickListener(this)
        binding.fallcarPointExplainLabel.setOnClickListener(this)
        binding.fallcarTransfer.setOnClickListener(this)
        binding.setting.setOnClickListener(this)
        binding.editXy.setOnClickListener(this)
        tooltipDialogContents()

        var adapter = ArrayAdapter.createFromResource(this, R.array.wkdth, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.fallcarPlaceTypeValue.adapter = adapter
        adapter = ArrayAdapter.createFromResource(this, R.array.fallcar, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.fallcarPointType.adapter = adapter

        binding.fallcarPointExplainValue.setOnKeyListener{ v: View?, code: Int?, _: Any? ->
            if(code == KeyEvent.KEYCODE_ENTER){
                val imm : InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        object:Handler(Looper.myLooper()!!){override fun handleMessage(msg: Message) {
            binding.fallcarPnameValue.textSize = binding.fallcarAutosizeSupprtTextViewSmall.textSize / (resources.displayMetrics.density)
        }
        }.sendEmptyMessageDelayed(0,500)

        binding.fallcarPnameValue.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text = if (p0?.isEmpty() == true) binding.fallcarPnameValue.hint.toString() else p0.toString()
                binding.fallcarAutosizeSupprtTextViewSmall.setText(text, TextView.BufferType.EDITABLE)
            }
            override fun afterTextChanged(p0: Editable?) {
                object:Handler(Looper.myLooper()!!){override fun handleMessage(msg: Message) {
                    binding.fallcarPnameValue.textSize = binding.fallcarAutosizeSupprtTextViewSmall.textSize / (resources.displayMetrics.density)
                }
                }.sendEmptyMessageDelayed(0,500)
                runFirstComplete()
                val string = binding.fallcarPnameValue.text.toString()

                resources.getStringArray(R.array.pName).iterator().forEach {
                    if(it==string) {
                        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(binding.fallcarPnameValue.windowToken,0)
                        return@forEach
                    }
                }

            }
        })

        binding.fallcarPnameValue.setOnKeyListener{v:View?, code:Int?,_:Any?->
            if(code == KeyEvent.KEYCODE_ENTER){
                val imm : InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        log(TAG,"attachClickListner -> ok")
    }

    private fun runFirstComplete(){
        if(binding.fallcarPnameValue.text.isEmpty()) return

        val rstList = ArrayList<String>()

        for( i in 0 until resources.getStringArray(R.array.pName).size){
            val pName = resources.getStringArray(R.array.pName)[i]
            val bResult = SoundSearcher().matchString(pName, binding.fallcarPnameValue.text.toString())
            if(bResult) rstList.add(pName)
        }

        if(rstList.size>0){
            val rstItem = arrayOfNulls<String>(rstList.size)
            rstList.toArray(rstItem)

            val adapter = ArrayAdapter<String>(this, R.layout.spinner_item_dropdown, rstItem)
            binding.fallcarPnameValue.setAdapter(adapter)


            binding.fallcarPnameValue.showDropDown()
            return
        }
        binding.fallcarPnameValue.dismissDropDown()
        return
    }


    @SuppressLint("MissingPermission")
    private fun saveResultValues() {
        val filter = "[^\uAC00-\uD7AFxfe0-9a-zA-Z\\s.,/()!@+~?><;*:\"'\\-\u3131-\u3163]"

        param[timeStamp] = SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(System.currentTimeMillis())
        param[address] = binding.fallcarAddress.text.toString()
        param[latitude] = binding.fallcarLatitudeValue.text.toString()
        param[longitude] = binding.fallcarLongitudeValue.text.toString()

        param[pointExplain] = binding.fallcarPointExplainValue.text.toString()
        param[pointExplain] = param[pointExplain]!!.replace(filter, "?")
        binding.fallcarPointExplainValue.setText(param[pointExplain])


        param[fm1_tnsckfwkdth_auto] = binding.fallcarPnameValue.text.toString()

        param[districtType] = binding.fallcarDistrictTypeValue.text.toString()
        param[placeType] = binding.fallcarPlaceTypeValue.selectedItem.toString()
        param[pointType] = if(binding.fallcarPointType.selectedItemPosition == 1) "추락지점" else "표지판"
        param[mainManager] = ""
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        param[phoneNo] = telephonyManager.line1Number.replace("+82", "0")
        log(TAG,"saveResultValues -> ok")
    }

    private fun checkResultValues() : Boolean{
        var isPname = false
        val tooltips: ArrayList<TooltipObject> = ArrayList()
        resources.getStringArray(R.array.pName).forEach { (if(it == param[fm1_tnsckfwkdth_auto]) isPname = true) }
        when{
            !isPicture -> {
                tooltips.add(TooltipObject(binding.imageView, null, "사진 촬영을 안했어요.", TooltipContentPosition.BOTTOM))
                tooltips.add(TooltipObject(binding.fallcarTakePic, null, "사진을 찍으세요.", TooltipContentPosition.TOP))
            }
            param[latitude]!!.length < 5 -> {
                tooltips.add(TooltipObject(binding.fallcarXyLayout, null, "좌표를 모르겠어요.", TooltipContentPosition.BOTTOM))
                tooltips.add(TooltipObject(binding.editXy, null, "위치를 입력하세요.", TooltipContentPosition.TOP))
            }
            !isPname -> tooltips.add(TooltipObject(binding.fallcarPnameValue, null, "소속을 알맞게 채워주세요.", TooltipContentPosition.TOP))
            param[pointExplain]!!.length < 2 -> tooltips.add(TooltipObject(binding.fallcarPointExplainValue, null, "장소에 대한 설명을 적어주세요.", TooltipContentPosition.TOP))
            binding.fallcarPlaceTypeValue.selectedItemPosition < 1 -> tooltips.add(TooltipObject(binding.fallcarPlaceTypeValue, null, "장소분류를 선택하세요.", TooltipContentPosition.TOP))
            binding.fallcarPointType.selectedItemPosition < 1 -> tooltips.add(TooltipObject(binding.fallcarPointType, null, "차량이 <font color=\"#FFC300\">추락한 위치</font>인가요? 아니면 추락한 곳 근처에 있는 <font color=\"#FFC300\">표지판</font> 인가요?", TooltipContentPosition.TOP))
        }
        if(tooltips.size>0){
            tooltipDialog?.show(this,supportFragmentManager,"checkInputValue",tooltips)
            return false
        }

        log(TAG,"checkResultValues -> ok(true)")
        return true
    }

    private fun requestEachPermission(name : String, permissionString : String) : Boolean{
        if(ActivityCompat.checkSelfPermission(this, permissionString) != PackageManager.PERMISSION_GRANTED){
            log(TAG,"requestEachPermission - $permissionString -> fail")
            Snackbar.make(binding.fallcarLogView, "$name 권한이 필요합니다.", Snackbar.LENGTH_INDEFINITE)
                .setAction("권한승인") {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,permissionString)){
                        Snackbar.make(binding.fallcarLogView, "권한거부 이력이 있습니다.\n설정을 들어가서 직접 변경해주세요.", Snackbar.LENGTH_INDEFINITE)
                            .setAction("설정"){
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

    private fun requestPermission(){
        log(TAG,"requestPermission start")

        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.INTERNET,       //Normal Permission
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,   //Normal Permission
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS
            ),0)

        log(TAG, "requestPermissions end")

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var checkPermission: Boolean = true
        for (i: Int in grantResults.indices) {
            log(TAG, " ${permissions[i]} -> ${if (grantResults[i] == PackageManager.PERMISSION_GRANTED) "ok" else "fail"}")
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                checkPermission = false
        }
        if(!checkPermission) {
            var sb = Snackbar.make(binding.fallcarLogView,"거부된 권한이 있습니다. 사용이 제한됩니다.",Snackbar.LENGTH_SHORT)
            if(tryCount++ > 1)
                sb.setAction("설정가기") {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                    tryCount = 0
                }
            sb.show()
        }
    }


    private fun checkLocationServicesStatus() : Boolean{
        log(TAG, "checkLocationServicesStatus()")
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun showDialogForLocationServiceSetting(){
        log(TAG,"showDialogForLocationServiceSetting()")
        val builder = AlertDialog.Builder(this@FallCarActivity)
        builder.setTitle("위치 비활성화")
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n위치 설정을 수정하시겠습니까?")
        builder.setCancelable(true)
        builder.setPositiveButton("설정"){_:DialogInterface, _:Int->
            val intent = Intent()
            intent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
            startActivity(intent)
        }
        builder.setNegativeButton("취소(종료"){ dia:DialogInterface, _:Int->
            dia.cancel()
            finish()
            Toast.makeText(mContext, "위치 서비스를 반드시 활성화하셔야 합니다", Toast.LENGTH_SHORT).show()
        }
        builder.setCancelable(false)
        builder.create().show()
    }

    private var photoUri : Uri? = null
    private fun sendTakePhotoIntent(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(takePictureIntent.resolveActivity(packageManager) != null ){
            var photoFile : File? = null
            try{
                photoFile = createImageFile()
            }catch(e: IOException){
                e.printStackTrace()
                log(TAG,"sendTakePhotoIntent exp -> $e")
            }

            if(photoFile != null){
                photoUri = FileProvider.getUriForFile(this, packageName, photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)
                startActivityForResult(takePictureIntent,345)
            }
        }
        log(TAG,"sendTakePhotoIntent -> ok")

    }
    private fun createImageFile() : File{
        val timeStamp = SimpleDateFormat("yyyy-mm-dd HH-mm", Locale.KOREA).format(System.currentTimeMillis())
        param[imageName] = timeStamp
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            param[imageName], ".jpg", storageDir
        )
        param[imageName] = image.name
        imageFilepath = image.absolutePath
        log(TAG,"createImageFile -> ok")
        return image
    }

    var isPicture = false
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 345 && resultCode == RESULT_OK){
            binding.imageView.setImageURI(photoUri)
            isPicture = true
            val i = Intent(this, KakaomapActivity::class.java)
            startActivity(i)
            val uriString = photoUri.toString()
            binding.editXy.visibility = View.VISIBLE
            if(uriString!=null && uriString.length>9)
                log(TAG, "onActivityResult - photoUri ${photoUri.toString().substring(0,9)}...${photoUri.toString().substring(uriString.length-7)}  ")
        }
        else{
            Toast.makeText(mContext, "촬영실패!! 재시도해주세요.", Toast.LENGTH_SHORT).show()
            log(TAG, "receive TakePhotoIntent -> fail")
        }
    }

    override fun onClick(v: View) {
        when(v){
            binding.fallcarTakePic -> {
                if(!requestEachPermission("사진 촬영을 위한 [카메라]", Manifest.permission.CAMERA)) return
                if(!requestEachPermission("촬영한 사진을 불러오기 위한 [저장공간 읽기]", Manifest.permission.READ_EXTERNAL_STORAGE)) return
                if(!requestEachPermission("촬영한 사진을 저장하기 위한 [저장공간 쓰기]", Manifest.permission.WRITE_EXTERNAL_STORAGE)) return
                if(!requestEachPermission("사진위치 파악을 위한 [위치정보1]", Manifest.permission.ACCESS_FINE_LOCATION)) return
                if(!requestEachPermission("사진위치 파악을 위한 [위치정보2]", Manifest.permission.ACCESS_COARSE_LOCATION)) return
                if(!checkLocationServicesStatus()) showDialogForLocationServiceSetting()

                binding.fallcarLatitudeValue.text = ""
                binding.fallcarLongitudeValue.text = ""
                binding.fallcarAddress.text = ""
                sendTakePhotoIntent()
            }
            binding.editXy ->{
                val i = Intent(this, KakaomapActivity::class.java)
                startActivity(i)
            }
            binding.setting-> binding.fallcarLogView.visibility = View.INVISIBLE - binding.fallcarLogView.visibility

            binding.fallcarTransfer ->{
                if(!requestEachPermission("게시자 식별을 위한 [휴대폰정보]", Manifest.permission.READ_PHONE_STATE)) return
                if(!requestEachPermission("게시자 식별을 위한 [휴대폰번호]", Manifest.permission.READ_PHONE_NUMBERS)) return
                saveResultValues()
                if(!checkResultValues()) return

                TranferData().execute()
                CheckTask(mContext).execute()
            }
        }
    }

    inner class TranferData() : AsyncTask<Unit,Unit,Array<String>>(){
        override fun doInBackground(vararg p0: Unit?): Array<String>? {
            var imageResult = 0
            var valuesResult = 10 * 0
            if(isRunningThread) return null
            Thread.sleep(3000)
            isRunningThread = true
            imageResult = HTTP(param,resources.getString(R.string.serverUrl)).
            DoFileUpload(imageFilepath, resources.getString(R.string.imageUploadUrlJsp))
            valuesResult = HTTP(param, resources.getString(R.string.serverUrl)).
            DoValuesUpload("", resources.getString(R.string.saveUrlJsp))
            isRunningThread = false

            return arrayOf("VALUES/IMAGE RESULT = $valuesResult / $imageResult ", "${imageResult+valuesResult}")
        }
        override fun onPostExecute(s:Array<String>){
            super.onPostExecute(s)
            log(TAG, s[0])
            handler.sendEmptyMessage(Integer.parseInt(s[1]))
        }
    }

    inner class CheckTask(c: Context?) : AsyncTask<Void, Void, Void>(){
        var asyncDialog : ProgressDialog? = null

        init{asyncDialog = ProgressDialog(c)}

        override fun onPreExecute() {
            asyncDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            asyncDialog!!.setMessage("전송중입니다...")
            asyncDialog!!.setCancelable(false)
            asyncDialog!!.show()
            super.onPreExecute()
        }
        override fun doInBackground(vararg p0: Void?): Void?{
            try{
                var interval = 0
                do{
                    if(interval++ > 30) break
                    Thread.sleep(500)
                }while(isRunningThread)
            }catch(e : Exception){
                e.printStackTrace()
            }
            return null
        }
        override fun onPostExecute(result: Void?) {
            asyncDialog!!.dismiss()
            super.onPostExecute(result)
        }

    }


    var handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var s = ""
            val v = msg.what
            if(v==11){
                s = "성공적으로 전송되었습니다."
                binding.fallcarTransfer.text = "전송완료!"
                binding.fallcarTransfer.setBackgroundResource(R.drawable.layout_border_label)

                object : Handler(Looper.getMainLooper()){
                    override fun handleMessage(msg: Message) {
                        super.handleMessage(msg)
                        binding.fallcarTransfer.setBackgroundResource(R.drawable.layout_border_yellow)
                        binding.fallcarTransfer.text = "전송하기"
                    }
                }.sendEmptyMessageDelayed(0,3000)
            }
            else{
                binding.fallcarTransfer.text = "전송싫패(재시도하기)"
                binding.fallcarTransfer.setBackgroundResource(R.drawable.layout_border_red)

                object : Handler(Looper.getMainLooper()){
                    override fun handleMessage(msg: Message) {
                        super.handleMessage(msg)
                        binding.fallcarTransfer.setBackgroundResource(R.drawable.layout_border_yellow)
                        binding.fallcarTransfer.text = "전송하기"
                    }
                }.sendEmptyMessageDelayed(0,3000)
            }
            if(v== 0) s =  "전송실패"
            if(v==10) s =  "사진전송실패"
            if(v== 1) s =  "입력실패"
            if(v==21) s =  "내용전송실패"
            Toast.makeText(mContext, s , Toast.LENGTH_SHORT).show()
        }
    }


    inner class NotificationReceiver : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {

            val lati = p1?.getStringExtra("latitude")
            val longi = p1?.getStringExtra("longitude")
            val address = p1?.getStringExtra("address")
            val districtPresent = p1?.getStringExtra("districtPresent")
            //val pointExplain = p1?.getStringExtra("pointExplain")
            log("NotificationReceiver", "BroadcastReceiver <- mapActivity")

            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    binding.fallcarLatitudeValue.text = lati
                    binding.fallcarLongitudeValue.text = longi
                    binding.fallcarAddress.text = address
                    binding.fallcarDistrictTypeValue.text = districtPresent
                    //if(pointExplain!="-")  binding.fallcarPointExplainValue.setText(pointExplain)
                }
            }.sendEmptyMessageDelayed(0, 500)
        }


    }

}

