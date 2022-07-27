package osy.kcg.mykotlin

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kcg.facillitykotlin.RV
import io.akndmr.ugly_tooltip.TooltipBuilder
import io.akndmr.ugly_tooltip.TooltipContentPosition
import io.akndmr.ugly_tooltip.TooltipDialog
import io.akndmr.ugly_tooltip.TooltipObject
import net.daum.mf.map.api.*
import osy.kcg.mykotlin.databinding.ActivityKakaomapBinding
import osy.kcg.utils.SoundSearcher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random

open class KakaomapActivity : AppCompatActivity(), MapView.CurrentLocationEventListener, MapView.MapViewEventListener, View.OnClickListener{
    val TAG ="KakaomapActivity"

    private var mapView : MapView? = null   // mapView
    var marker : MapPOIItem? = null // 시설위치지정 마커 (레드)
    var mContext : Context? = null  // Acticvity Context
    var latitude = .0   // Lat
    var longitude = .0  // Lon
    var isTransfer = false
    var dialog : Dialog? = null
    var tooltipDialog : TooltipDialog? = null

    private var rndurMap = HashMap<String, ArrayList<XYRndur>>()
    lateinit var binding: ActivityKakaomapBinding
    class XYRndur internal constructor(var x: Double, var y: Double, var type: String? = null, var id: String?=null)
    private var drawMode = false
    var drawModePoints = mutableListOf<MapPoint>()

    var mapViewContainer : ViewGroup? = null

    fun log(TAG : String, log : String) : Unit{
        try {
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    android.util.Log.i(TAG, log)
                    binding.kakaoLogTextView.setText("$log\n${binding.kakaoLogTextView.text.toString()}")
                }
            }.sendEmptyMessageDelayed(0,500)
        }catch(e:Exception){e.printStackTrace()}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKakaomapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        mContext = this
        loadMapView()
        attachLinstener()
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
            binding.kakaoHelp.setOnClickListener {
                if(drawMode){
                    val tooltips: ArrayList<TooltipObject> = ArrayList()
                    tooltips.add(
                        TooltipObject(
                            view = binding.kakaomapView,
                            title = "① 구역 그리기",
                            text = "<font color=\"#FFC300\">지도를 짧게 터치</font>하여 구역을 그릴 수 있어요.<br><font color=\"#FFC300\">3개 이상 포인트</font>를 표시해주세요.",
                            tooltipContentPosition = TooltipContentPosition.BOTTOM
                        )
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoPnameValue,
                            "① 소속","당신의 <font color=\"#FFC300\">소속</font>은 어디인가요?",
                            TooltipContentPosition.TOP)
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoTypeValue,
                            "② 구역정보",
                            "이 구역은 <font color=\"#FFC300\">무슨 구역</font>이에요? 선택해 주세요.",
                            TooltipContentPosition.BOTTOM)
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoNameValue,
                            "②₂ 상세정보",
                            "사진의 구역이 주소만으로는 이해하기 어려워요. <br><font color=\"#FFC300\">주소엔 표시되지 않는 위치</font>를 상세하게 입력해주세요.",
                            TooltipContentPosition.TOP)
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoAccept1,
                            "마지막!",
                            "이 버튼을 누르면 구역정보가 저장돼요",
                            TooltipContentPosition.TOP)
                    )
                    tooltipDialog?.show(this, supportFragmentManager, "SHOWCASE_TAG", tooltips)
                }

                if(!drawMode){
                    val tooltips: ArrayList<TooltipObject> = ArrayList()
                    tooltips.add(
                        TooltipObject(
                            view = binding.kakaomapView,
                            title = "① 위치 지정",
                            text = "촬영한 <font color=\"#FFC300\">시설물 위치</font>를 최대한 <font color=\"#FFC300\">정확하게 지정</font>해주세요.",
                            tooltipContentPosition = TooltipContentPosition.BOTTOM
                        )
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoMaptypeSwapButton,
                            "①₁ 지도변경","위치구분이 어렵나요? 위성지도도 볼 수 있어요.",
                            TooltipContentPosition.BOTTOM)
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoAddress2Value,
                            "② 구역정보",
                            "구역에 대한 정보가 표시돼요. <br>혹시..관리중인 구역인가요?",
                            TooltipContentPosition.TOP)
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoMakeRndurButton,
                            "②₂ 구역추가",
                            "관리중인 구역인데 지도에 표시가 안된다면 여기서 <font color=\"#FFC300\">구역을 생성</font>해 주세요.",
                            TooltipContentPosition.BOTTOM)
                    )
                    tooltips.add(
                        TooltipObject(
                            binding.kakaoAccept1,
                            "마지막!",
                            "이 버튼을 누르면 위치가 저장돼요.",
                            TooltipContentPosition.TOP)
                    )
                    tooltipDialog?.show(this, supportFragmentManager, "SHOWCASE_TAG", tooltips)
                }

            }
        }
    }



    private fun attachLinstener() {
        binding.kakaoMakeRndurButton.setOnClickListener(this)
        binding.kakaoUndoButton.setOnClickListener(this)
        binding.kakaoAccept1.setOnClickListener(this)
        binding.kakaoAccept2.setOnClickListener(this)
        binding.kakaoLogButton.setOnClickListener(this)
        binding.kakaoMaptypeSwapButton.setOnClickListener(this)
        tooltipDialogContents()

        var adapter = ArrayAdapter.createFromResource(this, R.array.rndur, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.kakaoTypeValue.adapter = adapter
        binding.kakaoPnameValue.setOnKeyListener{ v:View?, code:Int?, _:Any?->
            if(code == KeyEvent.KEYCODE_ENTER){
                val imm : InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        binding.kakaoPnameValue.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if(binding.kakaoPnameValue.text.isEmpty()) return
                val rstList = ArrayList<String>()
                val param = RV()
                for( i in 0 until param.getAllpName().size){
                    val pName = param.getAllpName()[i]
                    val bResult = SoundSearcher().matchString(pName, binding.kakaoPnameValue.text.toString())
                    if(bResult) rstList.add(pName)
                }
                if(rstList.size>0){
                    val rstItem = arrayOfNulls<String>(rstList.size)
                    rstList.toArray(rstItem)
                    binding.kakaoPnameValue.setAdapter(ArrayAdapter<String>(this@KakaomapActivity, R.layout.spinner_item_dropdown, rstItem))
                    binding.kakaoPnameValue.showDropDown()
                    if(rstList.size<5) (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(binding.kakaoPnameValue.windowToken,0)
                    return
                }
                binding.kakaoPnameValue.dismissDropDown()
                return
            }

        })

        binding.kakaoNameValue.setOnKeyListener{v:View?, code:Int?,event:Any?->
            if(code == KeyEvent.KEYCODE_ENTER){
                val imm : InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken,0)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                autoTextSize(binding.kakaoAutosizeSupprtTextViewSmall, binding.kakaoPnameValue)
                binding.kakaoNameValue.textSize = binding.kakaoNameLabel.textSize / (resources.displayMetrics.density)
            }

            override fun equals(other: Any?): Boolean {
                return super.equals(other)
            }
        }.sendEmptyMessageDelayed(0,500)


        mapView!!.setPOIItemEventListener(object : MapView.POIItemEventListener{
            override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {log(TAG,"POIItemDragg1")}
            override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?) {log(TAG,"POIItemDragg2")}
            override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?, p2: MapPOIItem.CalloutBalloonButtonType?) {log(TAG,"POIItemDragg3")}
            override fun onDraggablePOIItemMoved(p0: MapView?, p1: MapPOIItem?, p2: MapPoint?) {
                log(TAG,"POIItemDragg")
                p0!!.setMapCenterPoint(p2,true)
            }
        })

        log(TAG,"attach Linstener -> ok")
    }

    override fun onClick(v: View) {
        when(v.id){
            binding.kakaoMakeRndurButton.id ->{
                drawMode = !drawMode
                if(drawMode){ // ON
                    binding.kakaoUndoButton.visibility = View.VISIBLE
                    binding.kakaoLayoutForm1.visibility = View.GONE
                    binding.kakaoLayoutForm2.visibility = View.VISIBLE
                    drawModePoints = mutableListOf<MapPoint>()
                    binding.kakaoTitle.text = "구역정보 생성하기"
                    binding.kakaoAccept2.text = "구역정보 전송하기"
                    binding.kakaoMakeRndurButton.text = " 그리기 종료"
                    binding.kakaoRootLayout.setBackgroundColor(Color.argb(40,2550,230,130))
                    binding.xpng.visibility = View.INVISIBLE
                    binding.xpngGuide.text = "터치로 구역을 설정하세요."
                }
                else{   // OFF
                    binding.kakaoUndoButton.visibility = View.INVISIBLE
                    binding.kakaoLayoutForm2.visibility = View.GONE
                    binding.kakaoLayoutForm1.visibility = View.VISIBLE

                    mapView!!.poiItems.forEach { p-> if(p.tag==1000) mapView!!.removePOIItem(p) }
                    mapView!!.polylines.forEach { p -> if(p.tag==1500) mapView!!.removePolyline(p) }
                    mapView!!.poiItems.forEach { p-> if(p.tag==1501) mapView!!.removePOIItem(p) }
                    Thread{ drawRndur(0,1,2,3, 4, Color.argb(128, 255, 51, 0) ) }.start()
                    drawModePoints = mutableListOf<MapPoint>()
                    binding.kakaoTitle.text = "이 위치가 맞으신가요?"
                    binding.kakaoAccept1.text = "위치 입력 완료"
                    binding.kakaoMakeRndurButton.text = " 구역 생성"

                    binding.kakaoRootLayout.setBackgroundColor(Color.argb(0,0,0,0))
                    binding.xpng.visibility = View.VISIBLE
                    binding.xpngGuide.text = "여기에 맞춰주세요."
                }
            }
            binding.kakaoAccept1.id -> {
                val intent = Intent("osy.kcg.myKotlin")
                intent.putExtra("latitude", binding.kakaoLatitudeValue.text)
                intent.putExtra("longitude", binding.kakaoLongitudeValue.text)
                intent.putExtra("address", binding.kakaoAddressValue.text)
                intent.putExtra("type", marker!!.itemName)
                intent.putExtra("name", binding.kakaoAddress2Value.text)

                sendBroadcast(intent)
                finish()
            }
            binding.kakaoUndoButton.id -> {
                if(drawModePoints.isEmpty()) return
                drawModePoints.removeAt(drawModePoints.size-1)
                drawPolygon()
            }
            binding.kakaoAccept2.id -> {
                val filter = "[^\uAC00-\uD7AFxfe0-9a-zA-Z\\s.,/()!@+~?><;*:\"'\\-\u3131-\u3163]"

                val pnameValue = binding.kakaoPnameValue.text.toString().replace(Regex(filter), "?")
                val nameValue = binding.kakaoNameValue.text.toString().replace(Regex(filter), "?")

                if (pnameValue.contains("?") || nameValue.contains("?"))
                    Snackbar.make(binding.kakaoRootLayout, "특수문자는 삭제되어 전송됩니다.", Snackbar.LENGTH_SHORT).show()

                var pointValues = StringBuilder("")
                for (p in drawModePoints)
                    pointValues.append("/${p.mapPointGeoCoord.latitude},${p.mapPointGeoCoord.longitude}")
                pointValues = pointValues.replace(0, 1, "")

                log(TAG, "Accept2 : $pnameValue,$nameValue,$pointValues")
                if (pnameValue.length < 2 || nameValue.length < 2 || pointValues.length < 5 || binding.kakaoTypeValue.selectedItemPosition == 0) {
                    Snackbar.make(binding.kakaoRootLayout, "모든 필드를 입력해주세요.", Snackbar.LENGTH_SHORT).show()
                    return
                }
                val param = RV()
                param.serverUrl = resources.getString(R.string.serverUrl)
                param.savePointUrlJsp = resources.getString(R.string.savePointUrlJsp)
                param.ka_name = nameValue
                param.ka_pname = pnameValue
                param.ka_point = pointValues.toString()
                param.ka_type = binding.kakaoTypeValue.selectedItem.toString()
                param.ka_upTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(Calendar.getInstance().timeInMillis)
                Thread{
                    isTransfer = true
                    handler.sendEmptyMessage(1)
                    var result = HTTP(param).DoValuesUpload("point")
                    isTransfer = false
                }.start()

                binding.kakaoPnameValue.setText("")
                binding.kakaoNameValue.setText("")
                binding.kakaoTypeValue.setSelection(0)

            }
            binding.kakaoLogButton.id ->{
                var v = binding.kakaoLogTextView
                if(v.visibility == View.VISIBLE) v.visibility = View.INVISIBLE else v.visibility = View.VISIBLE
            }
            binding.kakaoMaptypeSwapButton.id ->{
                var t = mapView!!.mapType
                when(t){
                    MapView.MapType.Satellite -> {
                        mapView!!.mapType = MapView.MapType.Hybrid
                        binding.kakaoMaptypeSwapButton.text = "하이브리드"
                    }
                    MapView.MapType.Hybrid -> {
                        mapView!!.mapType = MapView.MapType.Standard
                        binding.kakaoMaptypeSwapButton.text = "일반지도"
                    }
                    MapView.MapType.Standard -> {
                        mapView!!.mapType = MapView.MapType.Satellite
                        binding.kakaoMaptypeSwapButton.text = "위성지도"
                    }
                }

            }
            else->{
                return
            }
        }
    }
    private var handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            log(TAG,"handler -> ${msg.what}")
            super.handleMessage(msg)
            when(msg.what){
                1 -> {
                    dialog = Dialog(mContext!!)
                    dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog!!.setContentView(ProgressBar(mContext))
                    dialog!!.setCanceledOnTouchOutside(false)
                    dialog!!.show()
                    this.sendEmptyMessageDelayed(0, 1000)
                }
                0 -> {
                    if(!isTransfer){
                        dialog!!.dismiss()
                        Snackbar.make(binding.kakaomapView, "구역등록 성공", Snackbar.LENGTH_SHORT).show()
                        onClick(binding.kakaoMakeRndurButton)
                        return
                    }
                    this.sendEmptyMessageDelayed(0, 1000)
                }
            }
        }
    }

    private fun autoTextSize(textView: TextView, view : View){
        val editText : EditText = view as EditText
        //textView.setText("가나", TextView.BufferType.EDITABLE)
        editText.textSize = textView.textSize / (resources.displayMetrics.density /*+ 0.2f*/)

        editText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text = if (p0?.isEmpty() == true) editText.hint.toString() else p0.toString()
                textView.setText(text, TextView.BufferType.EDITABLE)
            }
            override fun afterTextChanged(p0: Editable?) {
                editText.textSize = textView.textSize / (resources.displayMetrics.density /*+ 0.2f*/)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        log(TAG, "HashKey : ${com.kakao.util.maps.helper.Utility.getKeyHash(this)}")
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
                    while( (latitude<1 || longitude <1) && i++<60  ) Thread.sleep(500)
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
            mapViewContainer = binding.kakaomapView
            mapViewContainer!!.addView(mapView)
            mapView!!.setCurrentLocationEventListener(this)
            mapView!!.setMapViewEventListener(this)
            mapView!!.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving
            mapView!!.setZoomLevelFloat( -0.168175f, true)

            createMapMaker()
            getInitialLocation()
            Thread{ drawRndur(0,1,2,3, 4, Color.argb(128, 255, 51, 0))   }.start()

        }catch(e : Exception){
            e.printStackTrace()
            log(TAG, "loadMapView Exp: $e")
        }
        log(TAG, "loadMapView -> ok")
    }

    private fun createMapMaker(){
        marker = MapPOIItem()
        marker!!.tag = 1001
        marker!!.markerType = MapPOIItem.MarkerType.RedPin // 기본으로 제공하는 RedPin 마커 모양.
        marker!!.selectedMarkerType = MapPOIItem.MarkerType.RedPin // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
        marker!!.showAnimationType = MapPOIItem.ShowAnimationType.SpringFromGround
        marker!!.isShowDisclosureButtonOnCalloutBalloon = false
        log(TAG, "createMapMaker -> ok")
    }



        private fun getInitialLocation(){
        try {
            var gpsTracker : GpsTracker? = null // 처음 위치 검색을 위한 Google GPS. (1회 사용)
            gpsTracker = GpsTracker(this)
            latitude = gpsTracker.latitude
            longitude = gpsTracker.longitude

            val mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)
            mapView!!.setMapCenterPoint(mapPoint, true)
            marker!!.mapPoint = mapPoint
            mapView!!.addPOIItem(marker)
            mapView!!.selectPOIItem(marker, true)
            val matchPolygonName = getMatchPolygon(latitude, longitude)
            binding.kakaoAddress2Value.text = matchPolygonName[0]
            marker!!.itemName = matchPolygonName[1]
        }catch (e : Exception){log(TAG, "getInitialLocation Exp: $e")}
            log(TAG, "getInitialLocation ->: ok")
    }

    private fun getMatchPolygon(x:Double, y:Double): Array<String?> {
        try{
            val rndurIterator = rndurMap.keys.iterator()
            while(rndurIterator.hasNext()){
                val name = rndurIterator.next()
                val pMap = Polygon()
                for( r in rndurMap[name]!!) pMap.addPoint(r.x, r.y)
                if(pMap.isInside(x,y)){
                    var type = if(rndurMap[name]?.get(0)?.type!=null) rndurMap[name]?.get(0)?.type else ""
                    var id = if(rndurMap[name]?.get(0)?.id!=null) rndurMap[name]?.get(0)?.id else ""
                    log(TAG, "getMatchPolygon : Matched -> $id/$name ")
                    return arrayOf(name, type, id)
                }
            }
        }catch(e:Exception){
            e.printStackTrace()
            log(TAG, "getMatchPolygon exp: $e")
        }
        log(TAG, "getMatchPolygon -> uncharted")
        return arrayOf("미지정", "일반", "")
    }

    private fun drawRndur(NO_INDEX: Int, NAME_INDEX : Int,TYPE_INDEX : Int, LAT_INDEX : Int, LON_INDEX :Int, color : Int){
        try{
            val url = URL(resources.getString(R.string.serverUrl)+ resources.getString(R.string.getPointUrlJsp))
            var urlConnection = url.openConnection()
            urlConnection.connect()
            val inputStream = urlConnection.getInputStream()
            val br = BufferedReader(InputStreamReader(inputStream, "utf-8"))
            val dataHeader = br.readLine()
            log(TAG, "drawRndur -> $dataHeader")

            var polyline = MapPolyline()
            polyline.tag = 1000
            polyline.lineColor = color

            var matchRndur = ArrayList<XYRndur>()
            var prevPName = ""
            var pName = ""
            var type = ""
            var id = ""
            var lat = 1.0
            var lon = 2.0

            var v = br.readLine()
            while(v!=null){
                try{
                    val t = v.split(",")
                    pName = t[NAME_INDEX]
                    lat = t[LAT_INDEX].toDouble() // parseDouble
                    lon = t[LON_INDEX].toDouble()
                    type = t[TYPE_INDEX]
                    id = t[NO_INDEX]
                }catch (e : Exception){
                    log(TAG, "drawRndur1 Exp: $e")
                    log(TAG, "drawRndur1 : $v")
                    e.printStackTrace()
                }
                when {
                    prevPName == pName -> {
                        matchRndur.add(XYRndur(lat,lon))
                    }
                    prevPName !="" -> {
                        polyline = MapPolyline( )
                        polyline.tag = 1000
                        polyline.lineColor = Color.argb(128, Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))  //color
                        for(p : XYRndur in matchRndur)
                            polyline.addPoint(MapPoint.mapPointWithGeoCoord(p.x,p.y))
                        polyline.addPoint(MapPoint.mapPointWithGeoCoord(matchRndur[0].x, matchRndur[0].y))
                        mapView!!.addPolyline(polyline)

                        rndurMap[prevPName] = matchRndur
                        matchRndur = ArrayList()
                        matchRndur.add(XYRndur(lat,lon,type,id))
                    }
                    else -> {
                        matchRndur.add(XYRndur(lat,lon,type,id))
                    }
                }
                prevPName = pName
                v = br.readLine()
            }
        }catch (e : Exception){
            e.printStackTrace()
            log(TAG,"drawRndur2 exp: $e")
        }
        markerUpdate()
    }

    private fun markerUpdate(){
        var mapPoint = mapView!!.mapCenterPoint
        val name = getMatchPolygon(mapPoint.mapPointGeoCoord.latitude,mapPoint.mapPointGeoCoord.longitude)
        mapView!!.removePOIItem(marker)
        marker!!.mapPoint = mapPoint
        binding.kakaoAddress2Value.text = name[0]
        marker!!.itemName = name[1]
        binding.kakaoNameId.text = "ID : ${name[2]}"
        mapView!!.addPOIItem(marker)
        mapView!!.selectPOIItem(marker,true)
        log(TAG, "makerUpdate -> ok")
    }

    override fun onMapViewMoveFinished(mapView: MapView?, mapPoint: MapPoint?) {
        if(drawMode) return
        log(TAG, "★MapView onMapViewMoveFinished")

        val mapPointGeo = mapPoint!!.mapPointGeoCoord
        latitude = mapPointGeo.latitude
        longitude = mapPointGeo.longitude
        binding.kakaoLatitudeValue.text = latitude.toString()
        binding.kakaoLongitudeValue.text = longitude.toString()
        markerUpdate();

/*
        *//** 카카오 API를 통한 지오코딩. 실패확률이 높아서 보조용으로 사용*//*
        val reverseGeoCoder = MapReverseGeoCoder(resources.getString(R.string.kakaoMapKey), mapPoint, object : MapReverseGeoCoder.ReverseGeoCodingResultListener{
                override fun onReverseGeoCoderFoundAddress(p0: MapReverseGeoCoder?, s: String?) {
                    log(TAG, "(Kao)reversGeo success")
                    binding.kakaoAddressValue.text = s
                }
                override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {
                    log(TAG, "(Kao)reversGeo fail")
                }
            }, this)
        Thread{
            log(TAG, "(Kao)reversGeo start")
            reverseGeoCoder.startFindingAddress()}.start()*/


        Thread{
            var add = HTTP(null).Coord2Address("$latitude","$longitude",resources.getString(R.string.kakaoMapRESTKey),resources.getString(R.string.coord2address))
            var bundle = Bundle()
            var message = Message()
            message.data = bundle
            if(!add[0].isNullOrEmpty()) bundle.putString("add", add[0]) // 1순위 도로명
            else if(!add[1].isNullOrEmpty()) bundle.putString("add", add[1])    // 2순위 지번
            else{   // 3순위 구글주소
                try{
                    val address = Geocoder(mContext).getFromLocation(latitude,longitude,7)
                    val add = address[0].getAddressLine(0)
                    bundle.putString("add", "(인근)$add")
                    log(TAG, "(Std)reversGeo success")
                }catch(e: IOException){
                    e.printStackTrace()
                    log(TAG, "(Std)reversGeo exp: $e")
                }catch(e: IndexOutOfBoundsException){
                    e.printStackTrace()
                    log(TAG, "(Std)reversGeo notFound")
                }
            }
            object : Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    binding.kakaoAddressValue.text = msg.data.getString("add")
                }
            }.sendMessage(message)
        }.start()
    }

    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {
        //kakao로 처음 가져온 위치. 오차가 너무 커서 사용안하고 구글 위치로 대체했음.
        val mapPointGeo = p1!!.mapPointGeoCoord
        val lat = mapPointGeo.latitude
        val lon = mapPointGeo.longitude
//        log(TAG, "★MapView " + "onCurrentLocationUpdate ($lat,$lon), accuracy($p2), zoomLevel (${p0!!.zoomLevel}")
    }

    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {
        log(TAG, "★MapView onCurrentLocationDeviceHeadingupdate")
    }

    override fun onCurrentLocationUpdateFailed(p0: MapView?) {
        log(TAG, "★MapView onCurrentLocationUpdateFailed")
    }

    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {
        log(TAG, "★MapView onCurrentLocationUpdateCancelled")
    }

    override fun onMapViewInitialized(p0: MapView?) {
        log(TAG, "★MapView onMapViewInitialized")
    }

    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {
//        log(TAG, "★MapView onMapViewCenterPointMoved")
    }

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {
//        log(TAG, "★MapView onMapViewZoomLevelChanged")
    }


    private fun drawPolygon(){
        mapView!!.polylines.forEach { p -> if(p.tag==1500) mapView!!.removePolyline(p) }
        mapView!!.poiItems.forEach { p-> if(p.tag==1501) mapView!!.removePOIItem(p) }
        if(drawModePoints.size<1) return

        val polyline = MapPolyline()
        polyline.tag = 1500

        var maxX = 0.0
        var minX = 999.0
        var maxY = 0.0
        var minY =999.0

        for(i in 0 until drawModePoints.size) {
            polyline.addPoint(drawModePoints[i])

            var t = drawModePoints[i].mapPointGeoCoord.latitude
            if(t < minY) minY = t
            if(t > maxY) maxY = t
            t = drawModePoints[i].mapPointGeoCoord.longitude
            if(t < minX) minX = t
            if(t > maxX) maxX = t

            val markerYellow = MapPOIItem()
            markerYellow.itemName = "$i"
            markerYellow.tag = 1501
            markerYellow.markerType = MapPOIItem.MarkerType.YellowPin // 기본으로 제공하는 마커 모양.
            markerYellow.selectedMarkerType = MapPOIItem.MarkerType.YellowPin // 마커를 클릭했을때, 기본으로 제공하는 마커 모양.
            markerYellow.mapPoint = drawModePoints[i]
            markerYellow.isShowDisclosureButtonOnCalloutBalloon = false
            mapView!!.addPOIItem(markerYellow)
        }
        polyline.addPoint(drawModePoints[0])
        mapView!!.addPolyline(polyline)

        if(drawModePoints.size > 2) {
            val markerBlue = MapPOIItem()
            markerBlue.tag = 1501
            markerBlue.markerType = MapPOIItem.MarkerType.CustomImage
            markerBlue.customImageResourceId = R.mipmap.marker
            markerBlue.itemName = "center"
            markerBlue.isShowCalloutBalloonOnTouch = false
            markerBlue.mapPoint = MapPoint.mapPointWithGeoCoord(minY + (maxY - minY) / 2, minX + (maxX - minX) / 2)

            mapView!!.addPOIItem(markerBlue)
        }
    }

    override fun onMapViewSingleTapped(mapView : MapView?, mapPoint: MapPoint?) {
        log(TAG, "★MapView onMapViewSingleTapped")
        if(drawMode){
            try {
                drawModePoints.add(mapPoint!!)
                drawPolygon()
                return
            }catch(e : Exception){log(TAG, "onMapViewSingleTapped exp : $e")}
        }
        mapView!!.removePOIItem(marker)
        mapView.setMapCenterPoint(mapPoint, true)
    }

    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {
//        log(TAG, "★MapView onMapViewDoubleTapped")
    }

    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {
//        log(TAG, "★MapView onMapViewLongPressed")
    }

    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {
//        log(TAG, "★MapView onMapViewDragStarted")
    }

    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
//        log(TAG, "★MapView onMapViewDragEnded")
    }

}