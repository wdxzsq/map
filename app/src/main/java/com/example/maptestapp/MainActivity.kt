package com.example.maptestapp

import android.Manifest
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.maptestapp.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var map: MapView
    private val startPoint: GeoPoint = GeoPoint(56.135464, 47.2385113)
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var pointList: List<PlaceData> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = binding.mapView

        initMap()
        configurationMap()
        setZoomMultiTouch(true)
        getLocation()
        initPointList()

        pointList.forEach {
            setMarker(it.name, it.geoPoint)
        }

        binding.getLocation.setOnClickListener {
            map.controller.animateTo(locationOverlay.myLocation)
            map.controller.setZoom(14.0)
        } 

        val listPlace = listOf<String>(
            "Чебоксары, Торговый центр \"Каскад\"",
            "МТВ Центр",
            "ТРЦ \"Мадагаскар\"",
            "Торговый центр \"Мега Молл\"",
            "Чебоксары, Центральный Универмаг",
            "Торговый дом \"Новоюжный\"",
            "Чебоксары, Торговый центр \"Восточный\"",
            "Новочебоксарск, Торговый центр \"Вершина\"",
            "Новочебоксарск, Торговый центр \"Дубрава\"",
            "Канаш, Торговый центр \"Орион\"",
            "Канаш, Торговый центр \"Кристалл\"",
            "Торговый центр \"Ядрин\"",
            "Алатырь, Торговый центр \"Лабиринт\"",
            "Комсомольское, \"Торговый дом\"",
            "Моргауши, \"Торговый центр\"",
            "Чебоксары, Торговый центр \"Питер\"",
            "Чебоксары, Торговый центр \"Москва\"",

        )
        listPlace.forEach{
            val geocoder = Geocoder(this, Locale("RU"))
                .getFromLocationName(it, 1)
            geocoder.forEach { a->
                setMarker(it, GeoPoint(a.latitude, a.longitude), true)
            }
        }

    }

    private fun setZoomMultiTouch(b: Boolean) {
        map.setMultiTouchControls(b)
        map.overlays.add(RotationGestureOverlay(map))
    }

    private fun configurationMap() {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().osmdroidBasePath = filesDir
    }

    private fun initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setCenter(startPoint)
        map.controller.setZoom(14.0)
    }

    private fun getLocation() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()

        val imageDraw = ContextCompat.getDrawable(this, R.drawable.ic_pin_you)!!.toBitmap()
        locationOverlay.setDirectionAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        locationOverlay.setPersonAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        locationOverlay.setPersonIcon(imageDraw)
        locationOverlay.setDirectionIcon(imageDraw)

        map.overlays.add(locationOverlay)
    }

    private fun initPointList() {
        var id = 0
        pointList = listOf(
            PlaceData(id++, "Торговый центр \"Каскад\"", GeoPoint(56.135473,47.2390013)),
            PlaceData(id++, "Дом Торговли", GeoPoint(56.1349297,47.2467422)),
        )
    }

    private fun setMarker(name: String, geoPoint: GeoPoint, isNew: Boolean = false) {
        val marker = Marker(map)
        marker.position = geoPoint
        marker.title = name
        marker.icon =
            if (isNew) ContextCompat.getDrawable(this, R.drawable.ic_pin_prop_off)
            else ContextCompat.getDrawable(this, R.drawable.ic_pin_prop_on)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setOnMarkerClickListener { marker, _ ->
            buildRoad(marker.position) { h, m, s, a ->
                if (h>=1) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(marker.title)
                        .setMessage("""
                                    Адрес: $a
                                    Расстояние: $s км
                                    Время в пути: $h ч $m мин
                                    """.trimIndent())
                        .show()
                } else
                    MaterialAlertDialogBuilder(this)
                    .setTitle(marker.title)
                    .setMessage("""
                                    Адрес: $a
                                    Расстояние: $s км
                                    Время в пути: $m мин
                                    """.trimIndent())
                    .show()
            }
            return@setOnMarkerClickListener true
        }
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun buildRoad(endPoint: GeoPoint, callback: (h: Int, m: Int, s: Double, a: String) -> Unit) {
        map.overlays.removeAll { it is Polyline }

        CoroutineScope(Dispatchers.IO).launch {
            val geocoder = Geocoder(this@MainActivity, Locale("RU"))
                .getFromLocation(endPoint.latitude, endPoint.longitude, 1)

            val roadManager = OSRMRoadManager(this@MainActivity, System.getProperty("http.agent"))
            roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT) //
            val waypoints = arrayListOf(locationOverlay.myLocation ?: startPoint, endPoint)
            val road = roadManager.getRoad(waypoints)
            val roadOverlay = RoadManager.buildRoadOverlay(road)

            withContext(Dispatchers.Main){
                map.overlays.add(0, roadOverlay)
                map.invalidate()
                var h = floor(road.mDuration / 3600).toInt()
                var m = ((((road.mDuration - h*3600) / 60)*100).roundToInt() / 100.0).toInt()
                var s = (road.mLength * 100).roundToInt() / 100.0
                callback(h, m, s, geocoder[0].getAddressLine(0))
            }
        }
    }

}