package com.example.citycrater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.citycrater.databinding.ActivityDetailBinding
import com.example.citycrater.databinding.ActivityFriendsBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.markers.MarkerType
import com.example.citycrater.permissions.Permission
import com.example.citycrater.users.UserSessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding

    //MAPA
    private var map : MapView? = null
    private var currentLocationmarker: Marker? = null
    private lateinit var point: GeoPoint
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    //SENSORES
    private lateinit var mSensorManager: SensorManager
    private lateinit var mLightSensor: Sensor
    private lateinit var mLightSensorListener: SensorEventListener
    private var darkModeLum: Boolean = false
    private var lightModeLum: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().setUserAgentValue("com.example.citycrater")

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //vista de admin
        if(UserSessionManager.setUserView(binding.btnReports)){
            binding.btnReports.setOnClickListener {
                val intent = Intent(this, RequestsActivity::class.java)
                startActivity(intent)
            }
        }

        //INICIALIZACION
        initialize()

        //LISTENNERS DE LA PANTALLA
        setListenners()

        map!!.overlays.add(createOverlayEvents())
    }

    override fun onResume() {
        super.onResume()

        val intent = this.intent
        latitude = intent.getStringExtra("latitude")!!.toDouble()
        longitude = intent.getStringExtra("longitude")!!.toDouble()
        val direccion = MapManager.getAddressFromCoordinates(latitude, longitude)
        point = GeoPoint(latitude, longitude)

        binding.txtlocation.text = "Hueco reparado en: " + longitude + "  ;  "  + latitude

        map!!.onResume()

        mSensorManager.registerListener(mLightSensorListener, mLightSensor,
            SensorManager.SENSOR_DELAY_NORMAL)

        val mapController = map!!.controller
        mapController.setZoom(15)
        createMarker(point, direccion, null, R.drawable.baseline_location_on_24, MarkerType.CURRENT)
        currentLocationmarker?.let { map!!.overlays.add(it) }
        mapController.setCenter(point);

    }
    override fun onPause() {
        super.onPause()
        map!!.onPause()
        mSensorManager.unregisterListener(mLightSensorListener)
    }

    //INICIALIZACION
    private fun initialize(){
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        darkModeLum = false
        lightModeLum = true

        map = binding.osmMap
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.setMultiTouchControls(true)
    }

    //LISTENNERS DE LA PANTALLA
    private fun setListenners(){
        mLightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event!!.values[0] < 12) {
                    darkModeLum = true
                    map!!.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)

                } else {
                    lightModeLum = true
                    map!!.overlayManager.tilesOverlay.setColorFilter(null)
                }
                if(lightModeLum && darkModeLum){
                    lightModeLum = false
                    darkModeLum = false
                    map!!.setTileSource(TileSourceFactory.MAPNIK)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnReportBump.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, RegisterBumpActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay permiso de ubicacion", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }

        binding.btnMap.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay permiso de ubicacion", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }

        binding.btnFriends.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, FriendsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay permiso de ubicacion", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }
    }

    //FUNCIONES RELACIONADAS CON EL MAPA
    private fun createOverlayEvents(): MapEventsOverlay {
        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                return true
            }
        })
        return overlayEventos
    }

    fun createMarker(p: GeoPoint, title: String?, desc: String?, iconID: Int, markerType: String) {
        if (map != null) {

            lateinit var myIcon: Drawable
            if (iconID != 0) {
                myIcon = resources.getDrawable(iconID, this.theme)
            }

            currentLocationmarker = currentLocationmarker ?: Marker(map)
            title?.let { currentLocationmarker!!.title = it }
            desc?.let { currentLocationmarker!!.subDescription = it }
            currentLocationmarker!!.icon = myIcon
            currentLocationmarker!!.position = p
            currentLocationmarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }
}