package com.example.bunsilmul

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import net.daum.android.map.coord.MapCoordLatLng
import net.daum.android.map.location.MapViewLocationManager
import net.daum.mf.map.api.*
import net.daum.mf.map.api.MapPoint.GeoCoordinate
import net.daum.mf.map.api.MapPoint.mapPointWithGeoCoord
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



data class locations(
        var uid: String?,
        var location: user_location
)

data class user_location(
        var latitude: Double,
        var longitude: Double
)

data class message(
        var message: String,
        var uid: String
)

private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.249.18.152:8000/") // 마지막 / 반드시 들어가야 함
        .addConverterFactory(GsonConverterFactory.create()) // converter 지정
        .build() // retrofit 객체 생성

object LocationApiObject {
    val retrofitService: LocationInterface by lazy {
        retrofit.create(LocationInterface::class.java)
    }
}

class WantActivity : AppCompatActivity(), MapView.CurrentLocationEventListener, MapView.MapViewEventListener{
    private val LOG_TAG = "WantActivity"
    private lateinit var mapView : MapView
    private lateinit var mapViewContainer : ViewGroup
    private lateinit var polyline : MapPolyline
    private val GPS_ENABLE_REQUEST_CODE = 2001
    private val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)

    private var click = false

    private var locations = locations(null, user_location(.0, .0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wantactivity)
        mapView = MapView(this)
        mapViewContainer = findViewById<View>(R.id.map_view_want) as ViewGroup
        mapViewContainer.addView(mapView) // 지도를 띄움
//        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(36.373374, 127.359725), true);
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
        mapView.setZoomLevelFloat(3.0F, true)
        mapView.setMapRotationAngle(30F, true)
        mapView.setMapViewEventListener(this)
        mapView.setCurrentLocationEventListener(this)


        //지도에 이동 경로 나타내기
        val route_button = findViewById<Button>(R.id.route_button)
        route_button.setOnClickListener {
            //kakao map에 이동 경로 표시하기
            polyline = MapPolyline()
            polyline.tag = 1000
            polyline.lineColor = Color.argb(128, 255, 51, 0) // Polyline 컬러 지정.

            val call = LocationApiObject.retrofitService.GetLocation(FirebaseAuth.getInstance().uid)
            call.enqueue(object : retrofit2.Callback<Array<user_location>>{
                override fun onFailure(call: Call<Array<user_location>>, t: Throwable) {
                    Log.d("LocationCreate","Fail")
                }
                override fun onResponse(call: Call<Array<user_location>>, response: retrofit2.Response<Array<user_location>>) {
                    if(response.isSuccessful){
                        Log.d("Location","Success")
                        response.body()?.let{
                            for(user_location in it){
                                Log.d("Location",user_location.latitude.toString())
                                polyline.addPoint(MapPoint.mapPointWithGeoCoord(user_location.latitude, user_location.longitude))
                            }
                            mapView.addPolyline(polyline)
                            // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
                            val mapPointBounds = MapPointBounds(polyline.mapPoints)
                            val padding = 100 // px
                            mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding))
                        }
                    }
                    else{
                        Log.d("LocationCreate","response is error")
                    }
                }
            })


        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mapViewContainer.removeAllViews() // Activity 꺼지면 전부 제거
    }
    //
    override fun onPause(){
        super.onPause()
        if(click) {
            mapViewContainer.removeAllViews()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        click = true
        val intent = Intent(this, MainActivity_Map::class.java)
        startActivity(intent)
        finish()
    }

//    override fun onResume(){
//        super.onResume()
//        mapViewContainer.removeAllViews()
//    }

//    override fun onStop() {
//        super.onStop()
//        mapViewContainer.removeAllViews()
//    }

    override fun onCurrentLocationUpdate(p0: MapView?, currentLocation: MapPoint?, accuracyInMeters: Float) {
        val mapPointGeo = currentLocation?.mapPointGeoCoord
        Log.i(LOG_TAG, String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo?.latitude, mapPointGeo?.longitude, accuracyInMeters))
        //서버로 location 전달
        if (mapPointGeo != null) {
            locations.uid = FirebaseAuth.getInstance().currentUser?.uid
            locations.location.latitude = mapPointGeo.latitude
            locations.location.longitude = mapPointGeo.longitude

            val call = LocationApiObject.retrofitService.CreateLocation(locations)
            call.enqueue(object : retrofit2.Callback<message>{
                override fun onFailure(call: Call<message>, t: Throwable) {
                    Log.d("LocationCreate","Fail")
                }
                override fun onResponse(call: Call<message>, response: retrofit2.Response<message>) {
                    if(response.isSuccessful){
                        Log.d("Location","Success")
                        response.body()?.let{
                            if(it.message == "success"){
                                Log.d("LocationCreate","Success")
                            } else {
                                Log.d("LocationCreate","error")
                            }
                        }
                    }
                    else{
                        Log.d("LocationCreate","response is error")
                    }
                }
            })
        }
    }
    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {
        Log.d("회전","회전됨")
    }
    override fun onCurrentLocationUpdateFailed(p0: MapView?) {
    }
    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {
    }
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if( requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size){
            var check_result = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false
                    break
                }
            }
            if (check_result){
                mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
            }
        }
    }

    fun checkLocationServicesStatus(): Boolean{
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }
    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {
    }
    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {
    }
    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
    }
    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {
    }
    override fun onMapViewInitialized(p0: MapView?) {
    }
    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {
    }
    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {
    }
    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
    }
    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {
    }



}
