package osy.kcg.mykotlin

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.akndmr.ugly_tooltip.TooltipBuilder
import io.akndmr.ugly_tooltip.TooltipContentPosition
import io.akndmr.ugly_tooltip.TooltipDialog
import io.akndmr.ugly_tooltip.TooltipObject
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapPolyline
import net.daum.mf.map.api.MapView
import osy.kcg.mykotlin.databinding.ActivityKakaomap2Binding
import osy.kcg.utils.InputPointToTextAdapter
import osy.kcg.utils.SoundSearcher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


open class Kakaomap2Activity : AppCompatActivity(), MapView.CurrentLocationEventListener, MapView.MapViewEventListener, View.OnClickListener{

    companion object{
        const val TAG ="Kakaomap2Activity"
        const val name = 16
        const val pName = 17
        const val type = 18
        const val upTime = 19
        const val point = 20
        const val address = 21
        const val placeType = 22
        const val id = 23
    }

    private var mapView : MapView? = null   // mapView

    var mContext : Context? = null  // Acticvity Context
    var latitude = .0   // Lat
    var longitude = .0  // Lon
    var dialog : Dialog? = null
    var tooltipDialog : TooltipDialog? = null
    var isCrossLine = false
    var lastActionTime = Calendar.getInstance().timeInMillis

    private var rndurMap = HashMap< Map<Int, String>, ArrayList<XYRndur>>()
    class XYRndur internal constructor(var x: Double, var y: Double)
    lateinit var binding: ActivityKakaomap2Binding


    var drawModePoints = mutableListOf<MapPoint>()
    private fun calculDistance(p1:MapPoint, p2:MapPoint) : Double{
        val lat1 = p1.mapPointGeoCoord.latitude
        val lon1 = p1.mapPointGeoCoord.longitude
        val lat2 = p2.mapPointGeoCoord.latitude
        val lon2 = p2.mapPointGeoCoord.longitude
        val theta = lon1 - lon2
        var dist = sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * cos(deg2rad(theta))
        dist = acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515
        dist *= 1.609
        return dist * 1000 //km -> m
    }

    private fun ccw(p1 : MapPoint, p2 : MapPoint, p3 : MapPoint) : Int{
        val v = (p1.mapPointGeoCoord.longitude * p2.mapPointGeoCoord.latitude + p2.mapPointGeoCoord.longitude * p3.mapPointGeoCoord.latitude + p3.mapPointGeoCoord.longitude * p1.mapPointGeoCoord.latitude
                - (p1.mapPointGeoCoord.longitude * p3.mapPointGeoCoord.latitude + p2.mapPointGeoCoord.longitude * p1.mapPointGeoCoord.latitude + p3.mapPointGeoCoord.longitude * p2.mapPointGeoCoord.latitude))
        return if(v>0) 1 else if(v<0) -1 else 0
    }
    private fun isIntersect( p1:MapPoint, p2:MapPoint, p3:MapPoint, p4:MapPoint) : Boolean{
        val p1p2 =  ccw(p1, p2, p3) * ccw(p1, p2, p4)
        val p3p4 = ccw(p3, p4, p1) * ccw(p3, p4, p2)
        return p1p2 <= 0 && p3p4<=0
    }

    private var mapViewContainer : ViewGroup? = null

    // This function converts decimal degrees to radians
    open fun deg2rad(deg: Double) = deg * Math.PI / 180.0
    // This function converts radians to decimal degrees
    open fun rad2deg(rad: Double) = rad * 180 / Math.PI


    fun log(TAG : String, log : String){
        try {

            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    android.util.Log.i(TAG, log)
                    val text = "$log\n${binding.kakao2LogTextView.text.toString()}"
                    binding.kakao2LogTextView.setText(text)
                }
            }.sendEmptyMessageDelayed(0,500)
        }catch(e:Exception){e.printStackTrace()}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKakaomap2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        mContext = this
        loadMapView()
        attachListener()
        initTooltipDialog()
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
            .useCircleIndicator(true)
            .clickable(true) // Click anywhere to continue
            .useArrow(true) // Optional tooltip pointing arrow
            .setFragmentManager(this.supportFragmentManager)
            .setTooltipRadius(io.akndmr.ugly_tooltip.R.dimen.tooltip_radius) //Optional tooltip corder radius
            .showSpotlight(true) //Optional spotlight
            .showBottomContainer(false)
            .build()
    }
    private fun tooltipDialogContents(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            binding.kakao2Help.setOnClickListener {

                val tooltips: ArrayList<TooltipObject> = ArrayList()
                tooltips.add(
                    TooltipObject(
                        view = binding.kakao2mapView,
                        title = "① 구역 그리기",
                        text = "<font color=\"#FFC300\">지도를 짧게 터치</font>하여 구역을 그릴 수 있어요.<br><font color=\"#FFC300\">3개 이상 포인트</font>를 표시해주세요.",
                        tooltipContentPosition = TooltipContentPosition.BOTTOM
                    )
                )
                tooltips.add(
                    TooltipObject(
                        binding.kakao2PnameValue,
                        "② 소속","당신의 <font color=\"#FFC300\">소속</font>은 어디인가요?",
                        TooltipContentPosition.TOP)
                )
                tooltips.add(
                    TooltipObject(
                        binding.kakao2TypeValue,
                        "③ 구역정보",
                        "이 구역은 <font color=\"#FFC300\">무슨 구역</font>이에요? 선택해 주세요.",
                        TooltipContentPosition.BOTTOM)
                )
                tooltips.add(
                    TooltipObject(
                        binding.kakao2NameValue,
                        "④ 상세정보",
                        "사진의 구역이 주소만으로는 이해하기 어려워요. <br><font color=\"#FFC300\">주소엔 표시되지 않는 위치</font>를 상세하게 입력해주세요.",
                        TooltipContentPosition.TOP)
                )
                tooltips.add(
                    TooltipObject(
                        binding.kakao2Accept2,
                        "마지막!",
                        "이 버튼을 누르면 구역정보가 저장돼요",
                        TooltipContentPosition.TOP)
                )
                tooltipDialog?.show(this, supportFragmentManager, "SHOWCASE_TAG", tooltips)
            
            }
        }
    }



    private fun attachListener() {
        binding.kakao2UndoButton.setOnClickListener(this)
        binding.kakao2Accept2.setOnClickListener(this)
        binding.kakao2LogButton.setOnClickListener(this)
        binding.kakao2MaptypeSwapButton.setOnClickListener(this)
        binding.kakao2AddToTextButton.setOnClickListener(this)
        tooltipDialogContents()

        var adapter = ArrayAdapter.createFromResource(this, R.array.rndur, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.kakao2TypeValue.adapter = adapter
        adapter = ArrayAdapter.createFromResource(this, R.array.wkdth, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.kakao2PlaceType.adapter = adapter

        binding.kakao2PnameValue.setOnKeyListener{ v:View?, code:Int?, _:Any?->
            if(code == KeyEvent.KEYCODE_ENTER){
                val imm : InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        binding.kakao2PnameValue.dropDownVerticalOffset = -500
        binding.kakao2PnameValue.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if(binding.kakao2PnameValue.text.isEmpty()) return
                val rstList = ArrayList<String>()

                for( i in 0 until resources.getStringArray(R.array.pName).size){
                    val pName = resources.getStringArray(R.array.pName)[i]
                    val bResult = SoundSearcher().matchString(pName, binding.kakao2PnameValue.text.toString())
                    if(bResult) rstList.add(pName)
                }
                if(rstList.size>0){
                    val rstItem = arrayOfNulls<String>(rstList.size)
                    rstList.toArray(rstItem)
                    binding.kakao2PnameValue.setAdapter(ArrayAdapter<String>(this@Kakaomap2Activity, R.layout.spinner_item_dropdown, rstItem))
                    binding.kakao2PnameValue.showDropDown()
                    val string = binding.kakao2PnameValue.text.toString()
                    resources.getStringArray(R.array.pName).iterator().forEach {
                        if (it == string) {
                            (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                                .hideSoftInputFromWindow(binding.kakao2PnameValue.windowToken, 0)
                            return@forEach
                        }
                    }
                }
                else
                    binding.kakao2PnameValue.dismissDropDown()
            }

        })

        binding.kakao2NameValue.setOnKeyListener{ v:View?, code:Int?, _:Any?->
            if(code == KeyEvent.KEYCODE_ENTER){
                val imm : InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                autoTextSize(binding.kakao2AutosizeSupprtTextViewSmall, binding.kakao2PnameValue)
                binding.kakao2NameValue.textSize = binding.kakao2NameLabel.textSize / (resources.displayMetrics.density)
            }
        }.sendEmptyMessageDelayed(0,500)

        log(TAG,"attach Linstener -> ok")
    }

    override fun onClick(v: View) {
        when(v.id){

            binding.kakao2UndoButton.id -> {
                if(drawModePoints.isEmpty()) return
                drawModePoints.removeAt(drawModePoints.size-1)
                drawPolygon()
            }
            binding.kakao2Accept2.id -> {
                if(isCrossLine){
                    tooltipDialog?.show(this,supportFragmentManager, "checkInput", arrayListOf((TooltipObject(binding.kakao2mapView, null, "선을 교차해서 그릴 수 없어요.",TooltipContentPosition.BOTTOM))) )
                    return
                }
                val filter = "[^\uAC00-\uD7AFxfe0-9a-zA-Z\\s.,/()!@+~?><;*:\"'\\-\u3131-\u3163]"
                val nameValue = binding.kakao2NameValue.text.toString()
                val pnameValue = binding.kakao2PnameValue.text.toString()
                val placeTypeValue = binding.kakao2PlaceType.selectedItem.toString()
                if(pnameValue.contains(Regex(filter))){
                    Snackbar.make(binding.kakao2RootLayout, "특수문자는 삭제됩니다.", Snackbar.LENGTH_SHORT)
                        .setAction("확인"){
                            binding.kakao2PnameValue.setText(pnameValue.replace(Regex(filter), " "))
                        }.show()
                    return
                }



                var pointValues = StringBuilder("")
                for (p in drawModePoints)
                    pointValues.append("/${p.mapPointGeoCoord.latitude},${p.mapPointGeoCoord.longitude}")
                pointValues = pointValues.replace(0, 1, "")


                var pattern = "###.###############"
                while(pointValues.length > 1800){
                    pointValues = StringBuilder("")
                    for (p in drawModePoints) {
                        val lat = DecimalFormat(pattern).format( p.mapPointGeoCoord.latitude )
                        val lon = DecimalFormat(pattern).format( p.mapPointGeoCoord.longitude )
                        pointValues.append("/$lat,$lon")
                    }
                    pointValues = pointValues.replace(0, 1, "")
                    pattern = pattern.substring(0, pattern.length-1)
                    if(pattern.length < 10){
                        Snackbar.make(binding.kakao2RootLayout, "너무 포인트가 많습니다.", Snackbar.LENGTH_SHORT).show()
                        return
                    }
                }

                val tooltips: ArrayList<TooltipObject> = ArrayList()
                var isPname = false
                resources.getStringArray(R.array.pName).forEach { if(it == pnameValue) isPname = true }
                when{
                    drawModePoints.size < 3 -> tooltips.add(TooltipObject(binding.kakao2mapView, null, "3개 지점 이상 선택해주세요.",TooltipContentPosition.BOTTOM))
                    !isPname -> tooltips.add(TooltipObject(binding.kakao2PnameValue, null, "소속을 알맞게 채워주세요.",TooltipContentPosition.TOP))
                    binding.kakao2TypeValue.selectedItemPosition == 0 -> tooltips.add(TooltipObject( binding.kakao2TypeValue, null, "구역을 선택하세요.", TooltipContentPosition.TOP))
                    binding.kakao2PlaceType.selectedItemPosition == 0 -> tooltips.add(TooltipObject( binding.kakao2PlaceType, null, "장소유형을 선택하세요.", TooltipContentPosition.TOP))
                    nameValue.length < 2 -> tooltips.add(TooltipObject(binding.kakao2NameValue, null, "장소설명을 입력하세요.", TooltipContentPosition.TOP))
                }
                if(tooltips.size>0){
                    tooltipDialog?.show(this,supportFragmentManager,"checkInput",tooltips)
                    return
                }

                val param : HashMap<Int, String> = hashMapOf(
                    name to nameValue,
                    pName to pnameValue,
                    point to pointValues.toString(),
                    type to (binding.kakao2TypeValue.selectedItem.toString()),
                    upTime to SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(Calendar.getInstance().timeInMillis),
                    address to binding.kakao2Address.text.toString(),
                    placeType to placeTypeValue
                )

                dialog = Dialog(mContext!!)
                dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog!!.setContentView(ProgressBar(mContext))
                dialog!!.setCanceledOnTouchOutside(false)
                dialog!!.show()

                Thread{
                    val result = HTTP(param, resources.getString(R.string.serverUrl)).DoValuesUpload("point", resources.getString(R.string.savePointUrlJsp))
                    dialog!!.dismiss()
                    if(result == 10)
                        Snackbar.make(binding.kakao2mapView, "구역등록 성공", Snackbar.LENGTH_SHORT).show()
                    else
                        Snackbar.make(binding.kakao2mapView, "등록실패 (ERROR $result)", Snackbar.LENGTH_SHORT).show()
                    resetUI.sendEmptyMessageDelayed(0,1000)
                    log(TAG, "try upload Rndur -> $result")
                }.start()

            }
            binding.kakao2LogButton.id ->{
                val kakaoLogButton = binding.kakao2LogTextView
                if(kakaoLogButton.visibility == View.VISIBLE) kakaoLogButton.visibility = View.INVISIBLE else kakaoLogButton.visibility = View.VISIBLE
            }
            binding.kakao2MaptypeSwapButton.id ->{
                log(TAG, "mapType Change")
                when(mapView!!.mapType){
                    MapView.MapType.Satellite -> {
                        mapView!!.mapType = MapView.MapType.Hybrid
                        binding.kakao2MaptypeSwapButton.text = "하이브리드"
                    }
                    MapView.MapType.Hybrid -> {
                        mapView!!.mapType = MapView.MapType.Standard
                        binding.kakao2MaptypeSwapButton.text = "일반지도"
                    }
                    MapView.MapType.Standard -> {
                        mapView!!.mapType = MapView.MapType.Satellite
                        binding.kakao2MaptypeSwapButton.text = "위성지도"
                    }
                    else ->{}
                }

            }
            binding.kakao2AddToTextButton.id -> drawPolygonToText()
            else->{
                return
            }
        }
    }
    private var resetUI = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            binding.kakao2PnameValue.setText("")
            binding.kakao2NameValue.setText("")
            binding.kakao2TypeValue.setSelection(0)

            mapView!!.poiItems.forEach { p-> if(p.tag==1000) mapView!!.removePOIItem(p) }
            mapView!!.polylines.forEach { p -> if(p.tag==1500) mapView!!.removePolyline(p) }
            mapView!!.poiItems.forEach { p-> if(p.tag==1501) mapView!!.removePOIItem(p) }
            Thread{ drawRndur() }.start()
            drawModePoints = mutableListOf()  // mutableListOf<MapPoint>()

        }
    }

    private fun autoTextSize(textView: TextView, view : View){
        val editText : EditText = view as EditText
        try {
            editText.textSize = textView.textSize / (resources.displayMetrics.density /*+ 0.2f*/)
        }catch(e:Exception){}

        editText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try{
                    val text = if (p0?.isEmpty() == true) editText.hint.toString() else p0.toString()
                    textView.setText(text, TextView.BufferType.EDITABLE)
                }catch(e:Exception){}
            }
            override fun afterTextChanged(p0: Editable?) {
                try{
                    editText.textSize = textView.textSize / (resources.displayMetrics.density /*+ 0.2f*/)
                }catch(e:Exception){}
                log(TAG, "autoTextSize -> ok")
            }
        })
    }

    override fun onStart() {
        super.onStart()
//        log(TAG, "HashKey : ${com.kakao.util.maps.helper.Utility.getKeyHash(this)}")

        object : AsyncTask<Unit, Unit, Unit>(){
            var asyncDialog = ProgressDialog(mContext)

            override fun onPreExecute(){
                latitude = .0
                longitude = .0
                asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER )
                asyncDialog.setMessage("현재 위치를 불러오는 중입니다...")
                asyncDialog.setCancelable(false)
                asyncDialog.show()
                super.onPreExecute()
            }
            override fun doInBackground(vararg p0: Unit?) {
                try{
                    var i = 0
                    while( (latitude<1 || longitude <1) && i++<20  ) Thread.sleep(500)
                }catch(e : Exception){
                    e.printStackTrace()
                }
            }
            override fun onPostExecute(result: Unit?) {
                asyncDialog.dismiss()
                super.onPostExecute(result)
            }
        }.execute()
    }

    private fun loadMapView(){
        try{
            mapView = MapView(mContext)
            mapViewContainer = binding.kakao2mapView
            mapViewContainer!!.addView(mapView)
            mapView!!.setCurrentLocationEventListener(this)
            mapView!!.setMapViewEventListener(this)
            mapView!!.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving
            mapView!!.setZoomLevelFloat( -0.168175f, true)
            
            getInitialLocation()
            Thread{ drawRndur()   }.start()

        }catch(e : Exception){
            e.printStackTrace()
            log(TAG, "loadMapView Exp: $e")
        }
        log(TAG, "loadMapView -> ok")
    }



    private fun getInitialLocation(){
        try {
            val gpsTracker: GpsTracker? // 처음 위치 검색을 위한 Google GPS. (1회 사용)
            gpsTracker = GpsTracker(this)
            latitude = gpsTracker.latitude
            longitude = gpsTracker.longitude

            val mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)
            mapView!!.setMapCenterPoint(mapPoint, true)
            
        }catch (e : Exception){log(TAG, "getInitialLocation Exp: $e")}
        log(TAG, "getInitialLocation ->: ok")
    }


    private fun drawRndur(){

        val url = URL(resources.getString(R.string.serverUrl)+ resources.getString(R.string.getPointUrlJsp))
        val br : BufferedReader
        try{
            val urlConnection = url.openConnection()
            urlConnection.connect()
            val inputStream = urlConnection.getInputStream()
            br = BufferedReader(InputStreamReader(inputStream, "utf-8"))
        }catch (e : Exception){
            log(TAG, "drawRndur Error -> $e")
            return
        }

        val dataHeader = br.readLine()
        log(TAG, "drawRndur -> $dataHeader")

        var v = br.readLine()
        while(v != null){
            try{
                val matchRndur = ArrayList<XYRndur>()
                val t = v.split(",",limit=7)
                val districtType = t[3]
                val points = t[6]
                val color = when {
                    districtType.contains("사망사고") -> Color.argb(128, 0xe6, 0x7e, 0x22)
                    districtType.contains("위험구역") -> Color.argb(128, 0x9b, 0x59, 0xb6)
                    districtType.contains("다발구역") -> Color.argb(128, 0x29, 0x80, 0xb9)
                    districtType.contains("통제구역") -> Color.argb(128, 0x3f, 0x51, 0xb5)
                    else -> Color.argb(128, 0x16, 0xa0, 0x85)
                }

                val p = points.split("/")
                for(i : Int in p.indices) {
                    val t = p[i].split(",")
                    matchRndur.add(XYRndur(t[0].toDouble(), t[1].toDouble()))
                }
                val polyline = MapPolyline()
                polyline.tag = 1000
                polyline.lineColor = color
                for(p : XYRndur in matchRndur)
                    polyline.addPoint(MapPoint.mapPointWithGeoCoord(p.x,p.y))
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(matchRndur[0].x, matchRndur[0].y))
                mapView!!.addPolyline(polyline)

                hashMapOf(
                    name to t[0],
                    pName to t[1],
                    name to t[2],
                    type to t[3],
                    placeType to t[4],
                    address to t[5]
                ).also{rndurMap[it] = matchRndur}

            }catch (e : Exception){
                log(TAG, "drawRndur parsing -> text : $v\nerror -> $e")
            }
            v = br.readLine()
        }
    }

    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {
        val mapPointGeo = p1!!.mapPointGeoCoord
        latitude =  mapPointGeo.latitude
        longitude = mapPointGeo.longitude

    }

    private fun drawPolygon(){
        mapView!!.polylines.forEach {if(it.tag==1500) mapView!!.removePolyline(it) }
        mapView!!.polylines.forEach {if(it.tag==1600) mapView!!.removePolyline(it) }
        mapView!!.poiItems.forEach {if(it.tag==1501) mapView!!.removePOIItem(it) }

        val dSize = drawModePoints.size
        if(dSize < 1) return

        /** Create polyLine - Start */
        val polyline = MapPolyline()
        polyline.tag = 1500
        (0 until dSize).forEach{ polyline.addPoint(drawModePoints[it]) }
        polyline.lineColor = Color.rgb(0x29, 0x80, 0xb9)
        polyline.addPoint(drawModePoints[0])
        mapView!!.addPolyline(polyline)
        // Create polyLine - End

        /**Create Yellow Maker - Start */
        MapPOIItem().apply {
            itemName = "${dSize - 1}"
            tag = 1501
            markerType = MapPOIItem.MarkerType.YellowPin
            selectedMarkerType = MapPOIItem.MarkerType.YellowPin
            mapPoint = drawModePoints[dSize-1]
            isShowDisclosureButtonOnCalloutBalloon = false
            mapView!!.addPOIItem(this)
        }
        //Create Yellow Maker - End

        if(dSize < 3) {
            binding.kakao2RndurLinelength.text = ""
            binding.kakao2RndurArea.text = ""
            return
        }

        /**Calculate surface Length(m) - start */
        var lineLength = .0
        for(i in 1..dSize)
            lineLength += calculDistance(drawModePoints[i - 1], drawModePoints[i % dSize])
        var text = DecimalFormat("###,###").format(lineLength )+"m"
        binding.kakao2RndurLinelength.text = text
        //Calculate surface Length - End

        /**Calculate inner size(m²) - start */
        var innerArea = .0
        val referX = drawModePoints[0].mapPointGeoCoord.longitude
        val referY = drawModePoints[0].mapPointGeoCoord.latitude
        var preX = drawModePoints[1].mapPointGeoCoord.longitude
        var preY = drawModePoints[1].mapPointGeoCoord.latitude
        var centerX = ((referX+preX)* ((referX*preY)-(preX*referY)))
        var centerY = ((referY+preY)* ((referX*preY)-(preX*referY)))
        for( i in 2 until dSize){
            val curX = drawModePoints[i].mapPointGeoCoord.longitude
            val curY = drawModePoints[i].mapPointGeoCoord.latitude
            var lengthPreX = calculDistance( drawModePoints[i-1], MapPoint.mapPointWithGeoCoord(preY,referX) )
            var lengthCurX = calculDistance( drawModePoints[i], MapPoint.mapPointWithGeoCoord(curY, referX) )
            var lengthPreY = calculDistance( drawModePoints[i-1], MapPoint.mapPointWithGeoCoord(referY ,preX) )
            var lengthCurY = calculDistance( drawModePoints[i], MapPoint.mapPointWithGeoCoord(referY ,curX) )
            lengthPreX = if(preX < referX ) -lengthPreX else lengthPreX
            lengthCurX = if(curX < referX ) -lengthCurX else lengthCurX
            lengthPreY = if(preY < referY ) -lengthPreY else lengthPreY
            lengthCurY = if(curY < referY ) -lengthCurY else lengthCurY
            innerArea += lengthPreX*lengthCurY
            innerArea -= lengthPreY*lengthCurX

            centerX += ((lengthPreX+lengthCurX)* ((lengthPreX*lengthCurY)-(lengthCurX*lengthPreY)))
            centerY += ((lengthPreY+lengthCurY)* ((lengthPreX*lengthCurY)-(lengthCurX*lengthPreY)))
//            innerArea += ccw(XYRndur(.0,.0), XYRndur(lengthPreX,lengthPreY), XYRndur(lengthCurX,lengthCurY) )
            preX = drawModePoints[i].mapPointGeoCoord.longitude
            preY = drawModePoints[i].mapPointGeoCoord.latitude
        }
        centerX = centerX / (270000 * innerArea) + referX
        centerY = centerY / (330000 * innerArea) + referY
        innerArea = if(innerArea > 0) innerArea/2 else -innerArea/2

        text = when {
            innerArea > 1000000000 -> DecimalFormat("###,###").format(  (innerArea/1000000) )+"km²"
            innerArea > 1000000 -> DecimalFormat("###,###.##").format(  (innerArea/1000000) )+"km²"
            innerArea > 10000 -> DecimalFormat("###,###.##").format(  (innerArea/10000) )+"ha"
            else -> DecimalFormat("###,###").format(  (innerArea) )+"m²"
        }
        binding.kakao2RndurArea.text = text
        //Calculate inner size(m²) - End

        /**create center Marker - start */
        val markerCenter = MapPOIItem()
        markerCenter.tag = 1501
        markerCenter.markerType = MapPOIItem.MarkerType.CustomImage
        markerCenter.customImageResourceId = R.mipmap.marker
        markerCenter.itemName = "center"
        markerCenter.isShowCalloutBalloonOnTouch = false
        markerCenter.mapPoint = MapPoint.mapPointWithGeoCoord(centerY , centerX) // center
        mapView!!.addPOIItem(markerCenter)
        object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                var cTime = Calendar.getInstance().timeInMillis
                if(cTime - lastActionTime > 2000) {
                    getAddress(centerY, centerX)
                    lastActionTime = Calendar.getInstance().timeInMillis
                }
            }
        }.sendEmptyMessageDelayed(0, 2000)
        //create center Marker - End


        /** Polygon Validity Check - End */
        binding.kakao2crossView.visibility = View.INVISIBLE
        isCrossLine = false
        for(j in 1 until dSize-1)
            for( k in j+2..dSize){
                if(j==1 && k ==dSize) continue
                if(!isIntersect(drawModePoints[j-1], drawModePoints[j], drawModePoints[k%dSize], drawModePoints[k-1])) continue
                if(!isCrossLine) {
                    isCrossLine = true
                    binding.kakao2crossView.visibility = View.VISIBLE
                }
                MapPolyline().apply {
                    tag = 1600
                    addPoint(drawModePoints[j-1])
                    addPoint(drawModePoints[j])
                    mapView!!.addPolyline(this)
                }
                MapPolyline().also {
                    it.tag = 1600
                    it.addPoint(drawModePoints[k%dSize])
                    it.addPoint(drawModePoints[k-1])
                    mapView!!.addPolyline(it)
                }
            }
        // Polygon Validity Check - End
    }

    private fun getAddress(lat : Double, lon : Double){
        Thread{
            val add = HTTP(null,resources.getString(R.string.serverUrl)).
            Coord2Address("$lat","$lon",resources.getString(R.string.kakaoMapRESTKey),resources.getString(R.string.coord2address))
            val bundle = Bundle()
            val message = Message()
            message.data = bundle
            if(!add[0].isNullOrEmpty()) bundle.putString("add", add[0]) // 1순위 도로명
            else if(!add[1].isNullOrEmpty()) bundle.putString("add", add[1])    // 2순위 지번
            else{   // 3순위 구글주소
                try{
                    val address = Geocoder(mContext).getFromLocation(lat,lon,7)
                    val add2 = address[0].getAddressLine(0)
                    bundle.putString("add", "(인근)$add2")
                    log(KakaomapActivity.TAG, "(Std)reversGeo success")
                }catch(e: IOException){
                    e.printStackTrace()
                    log(KakaomapActivity.TAG, "(Std)reversGeo exp: $e")
                }catch(e: IndexOutOfBoundsException){
                    e.printStackTrace()
                    log(KakaomapActivity.TAG, "(Std)reversGeo notFound")
                }
            }
            object : Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    binding.kakao2Address.text = msg.data.getString("add")
                }
            }.sendMessage(message)
        }.start()
    }

    private fun drawPolygonToText(){
        Handler(Looper.getMainLooper()).post {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.customdialog_input_view)
            dialog.setCancelable(false)
            val listView = dialog.findViewById<ListView>(R.id.inputpoint_listView)
            var ary = ArrayList<MutableList<String>>()
            val initAry = mutableListOf("", "")
            ary.add(initAry)
            var adapter = InputPointToTextAdapter(this, ary)
            listView.adapter = adapter
            adapter.setItem(adapter.count - 1, initAry)
            dialog.show()

            dialog.findViewById<Button>(R.id.input_view_plus).setOnClickListener {
                val inputLat : Double = try{
                    dialog.findViewById<EditText>(R.id.inputpoint_lat).text.toString().toDouble()
                }catch(e : Exception){1.0}
                if(inputLat<30 || inputLat > 40){
                    Toast.makeText(this,"위도가 잘못되었어요. 확인해주세요.",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val inputLon : Double = try{
                    dialog.findViewById<EditText>(R.id.inputpoint_lon).text.toString().toDouble()
                }catch(e : Exception){1.0}
                if(inputLon>140 || inputLon < 120){
                    Toast.makeText(this,"경도가 잘못되었어요. 확인해주세요.",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else{
                    ary.add(mutableListOf("$inputLat", "$inputLon"))
                    adapter.setItem(0, mutableListOf("",""))
                    listView.adapter = adapter
                }
            }

            dialog.findViewById<Button>(R.id.input_view_minus).setOnClickListener {
                val size = adapter.count
                if(size>1) ary.removeAt(adapter.count - 1)
                listView.adapter = adapter
            }
            dialog.findViewById<Button>(R.id.inputpoint_accept).setOnClickListener {
                val inputLat : Double = try{
                    dialog.findViewById<EditText>(R.id.inputpoint_lat).text.toString().toDouble()
                }catch(e : Exception){1.0}
                if(inputLat<30 || inputLat > 40){
                    Toast.makeText(this,"위도가 잘못되었어요. 확인해주세요.",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val inputLon : Double = try{
                    dialog.findViewById<EditText>(R.id.inputpoint_lon).text.toString().toDouble()
                }catch(e : Exception){1.0}
                if(inputLon>140 || inputLon < 120){
                    Toast.makeText(this,"경도가 잘못되었어요. 확인해주세요.",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else
                    ary.add(mutableListOf("$inputLat", "$inputLon"))

                for(i in 1 until adapter.count) {
                    val c = adapter.getItem(i)
                    drawModePoints.add(MapPoint.mapPointWithGeoCoord(c[0].toDouble(),c[1].toDouble()))
                }
                val mapPoint = MapPoint.mapPointWithGeoCoord(inputLat,inputLon)
                mapView!!.setMapCenterPoint(mapPoint, true)
                drawPolygon()
                dialog.dismiss()
            }
            dialog.findViewById<Button>(R.id.inputpoint_cancel).setOnClickListener {dialog.dismiss()}
        }

    }

    override fun onMapViewSingleTapped(mapView : MapView?, mapPoint: MapPoint?) {
        log(TAG, "${mapPoint!!.mapPointGeoCoord.latitude}, ${mapPoint.mapPointGeoCoord.longitude}")
        try {
            drawModePoints.add(mapPoint)
            drawPolygon()
        }catch(e : Exception){log(TAG, "onMapViewSingleTapped exp : $e")}

    }
    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {}
    override fun onCurrentLocationUpdateFailed(p0: MapView?) {}
    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {}
    override fun onMapViewInitialized(p0: MapView?) {}
    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}
    override fun onMapViewMoveFinished(mapView: MapView?, mapPoint: MapPoint?) {}
    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}
}