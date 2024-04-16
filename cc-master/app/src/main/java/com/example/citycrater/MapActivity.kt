package com.example.citycrater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.citycrater.databinding.ActivityMapBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.markers.MarkerType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
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
import java.io.IOException

class MapActivity : AppCompatActivity() {

    lateinit var binding: ActivityMapBinding

    //MAPA
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var map : MapView? = null
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private var currentLocationmarker: Marker? = null


    //SENSORES
    private lateinit var mSensorManager: SensorManager
    private lateinit var mLightSensor: Sensor
    private lateinit var mLightSensorListener: SensorEventListener
    private var darkModeLum: Boolean = false
    private var lightModeLum: Boolean = true

    //RUTAS
    lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var bumpLocationMarker: Marker? = null
    private var settinOrigin: Boolean = false
    private var settingDestination: Boolean = false

    //GEOCODER
    var mGeocoder: Geocoder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().setUserAgentValue("com.example.citycrater")

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //INICIALIZACION DE VARIABLES
        initialize()

        //LISTENER UBICACION ORIGEN
        listennerGeocoderorigin()

        //LISTENER UBICACION DESTINO
        listennerGeocoderDestination()

        //LISTENNERS DE LA PANTALLA
        setListenners()

        map!!.overlays.add(createOverlayEvents())


    }

    override fun onResume() {
        super.onResume()
        map!!.onResume()



        mSensorManager.registerListener(mLightSensorListener, mLightSensor,
            SensorManager.SENSOR_DELAY_NORMAL)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                Log.i("LOCATION", "onSuccess location")
                if (location != null) {
                    Log.i("LOCATION", "Longitud: " + location.longitude)
                    Log.i("LOCATION", "Latitud: " + location.latitude)
                    val mapController = map!!.controller
                    mapController.setZoom(15)
                    val startPoint = GeoPoint(location.latitude, location.longitude);
                    mapController.setCenter(startPoint);
                } else {
                    Log.i("LOCATION", "FAIL location")
                }
            }
        }


        var bumpLocations: List<Pair<Double, Double>> = readJsonPairs("BumpLocations.json")
        if(bumpLocations.isNotEmpty()){
            bumpLocations.forEachIndexed { index, bumpLocation ->
                val (x, y) = bumpLocation
                bumpLocationMarker = createMarkerRetMark(GeoPoint(x, y), "null", null, R.drawable.baseline_location_pin_25)
                bumpLocationMarker?.setOnMarkerClickListener { marker, mapView ->
                    val intent = Intent(this, ReportFixedActivity::class.java)
                    intent.putExtra("latitude", x.toString())
                    intent.putExtra("longitude", y.toString())
                    startActivity(intent)
                    true
                }
                bumpLocationMarker?.let { map!!.overlays.add(it) }

            }
        }


    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        map!!.onPause()
        mSensorManager.unregisterListener(mLightSensorListener)
    }

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    //LISTENER UBICACION ORIGEN
    private fun listennerGeocoderorigin (){
        binding.origin.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val addressString = binding.origin.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        if (map != null && mGeocoder != null) {
                            val addresses = mGeocoder!!.getFromLocationName(addressString, 1)
                            if (addresses != null && addresses.isNotEmpty()) {
                                removeRoute()
                                val addressResult = addresses[0]
                                val position = GeoPoint(addressResult.latitude, addressResult.longitude)

                                Log.i("Geocoder", "Dirección encontrada: ${addressResult.getAddressLine(0)}")
                                Log.i("Geocoder", "Latitud: ${addressResult.latitude}, Longitud: ${addressResult.longitude}")
                                binding.origin.hint = addressResult.getAddressLine(0)

                                //Agregar Marcador al mapa
                                createMarker(position, addressString, null, R.drawable.baseline_location_on_24, MarkerType.ORIGIN)
                                originMarker?.let { map!!.overlays.add(it) }
                                map!!.controller.setCenter(originMarker!!.position)


                            } else {
                                Log.i("Geocoder", "Dirección no encontrada:" + addressString)
                                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "La dirección esta vacía", Toast.LENGTH_SHORT).show()

                }
            }
            true
        }
    }

    //LISTENER UBICACION DESTINO
    private fun listennerGeocoderDestination (){
        binding.destination.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val addressString = binding.destination.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        if (map != null && mGeocoder != null) {
                            val addresses = mGeocoder!!.getFromLocationName(addressString, 1)
                            if (addresses != null && addresses.isNotEmpty()) {
                                removeRoute()
                                val addressResult = addresses[0]
                                val position = GeoPoint(addressResult.latitude, addressResult.longitude)

                                Log.i("Geocoder", "Dirección encontrada: ${addressResult.getAddressLine(0)}")
                                Log.i("Geocoder", "Latitud: ${addressResult.latitude}, Longitud: ${addressResult.longitude}")
                                binding.destination.hint = addressResult.getAddressLine(0)

                                //Agregar Marcador al mapa
                                createMarker(position, addressString, null, R.drawable.baseline_location_on_24, MarkerType.DESTINATION)
                                destinationMarker?.let { map!!.overlays.add(it) }
                                map!!.controller.setCenter(destinationMarker!!.position)



                            } else {
                                Log.i("Geocoder", "Dirección no encontrada:" + addressString)
                                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "La dirección esta vacía", Toast.LENGTH_SHORT).show()

                }
            }
            true
        }
    }

    //INICIALIZACION DE VARIABLES
    private fun initialize(){
        roadManager = OSRMRoadManager(this, "ANDROID")
        mGeocoder = Geocoder(this)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        darkModeLum = false
        lightModeLum = true

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        map = binding.osmMap
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.setMultiTouchControls(true)
    }

    private fun createLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
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
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                Log.i("LOCATION", "Location update in the callback: $location")
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    createMarker(point, "you", null, R.drawable.baseline_location_pin_24, MarkerType.CURRENT)
                    currentLocationmarker?.let { map!!.overlays.add(it) }
                    //map!!.controller.setCenter(currentLocationmarker!!.position)



                    currentLocationmarker!!.setOnMarkerClickListener { marker, mapView ->
                        Log.i("FLAGLISTENER", "HOLA")
                        true  // Returning true consumes the click event
                    }

                }
            }
        }

        binding.btnGoRoute.setOnClickListener {
            listennerBtnGo()
        }

        binding.btnGetDestination.setOnClickListener {
            settingDestination = true
            settinOrigin = false
            Toast.makeText(this, "selecciona destino en mapa", Toast.LENGTH_SHORT).show()
        }

        binding.btnGetOrigin.setOnClickListener {
            settingDestination = false
            settinOrigin = true
            Toast.makeText(this, "selecciona origen en mapa", Toast.LENGTH_SHORT).show()
        }

        binding.btnFriends.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            startActivity(intent)
        }

        binding.btnMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        binding.btnReportBump.setOnClickListener {
            val intent = Intent(this, RegisterBumpActivity::class.java)
            startActivity(intent)
        }

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }



    }

    private fun listennerBtnGo(){
        var startPoint: GeoPoint
        var destinationPoint: GeoPoint

        settinOrigin = false
        settingDestination = false

        if(originMarker != null && destinationMarker != null ){
            startPoint = GeoPoint(originMarker!!.position!!.latitude, originMarker!!.position!!.longitude);
            destinationPoint = GeoPoint(destinationMarker!!.position!!.latitude, destinationMarker!!.position!!.longitude);
            drawRoute(startPoint,destinationPoint)
        }
    }


    //FUNCIONES RELACIONADAS AL MAPA
    private fun createOverlayEvents(): MapEventsOverlay {
        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                longPressOnMap(p)
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

            when (markerType) {
                MarkerType.ORIGIN -> {
                    originMarker = originMarker ?: Marker(map)
                    title?.let { originMarker!!.title = it }
                    desc?.let { originMarker!!.subDescription = it }
                    originMarker!!.icon = myIcon
                    originMarker!!.position = p
                    originMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                }
                MarkerType.DESTINATION -> {
                    destinationMarker = destinationMarker ?: Marker(map)
                    title?.let { destinationMarker!!.title = it }
                    desc?.let { destinationMarker!!.subDescription = it }
                    destinationMarker!!.icon = myIcon
                    destinationMarker!!.position = p
                    destinationMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                MarkerType.CURRENT -> {
                    currentLocationmarker = currentLocationmarker ?: Marker(map)
                    title?.let { currentLocationmarker!!.title = it }
                    desc?.let { currentLocationmarker!!.subDescription = it }
                    currentLocationmarker!!.icon = myIcon
                    currentLocationmarker!!.position = p
                    currentLocationmarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            }
        }
    }

    private fun createMarkerRetMark(p: GeoPoint, title: String?, desc: String?, iconID: Int): Marker? {
        var marker: Marker? = null
        if (map != null) {
            marker = Marker(map)
            title?.let { marker.title = it }
            desc?.let { marker.subDescription = it }
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, this.theme)
                marker.icon = myIcon
            }
            marker.position = p
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        return marker
    }

    private fun longPressOnMap(p: GeoPoint) {
        val address = MapManager.getAddressFromCoordinates(p.latitude, p.longitude)

        if(settinOrigin && !settingDestination){
            //SELECCIONAR ORIGEN
            removeRoute()
            createMarker(p, address, null, R.drawable.baseline_location_on_24, MarkerType.ORIGIN)
            originMarker?.let { map!!.overlays.add(it) }
            map!!.controller.setCenter(originMarker!!.position)
            binding.origin.setText(address)
        }else if (settingDestination && !settinOrigin) {
            //SELECCIONAR DESTINO
            removeRoute()
            createMarker(p, address, null, R.drawable.baseline_location_on_24, MarkerType.DESTINATION)
            destinationMarker?.let { map!!.overlays.add(it) }
            map!!.controller.setCenter(destinationMarker!!.position)
            binding.destination.setText(address)
        }else{
            Toast.makeText(this, "elige origen o destino", Toast.LENGTH_SHORT).show()
        }
    }
    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        Log.i("OSM_acticity", "Route length: ${road.mLength} klm")
        Log.i("OSM_acticity", "Duration: ${road.mDuration / 60} min")

        if (map != null) {
            roadOverlay?.let { map!!.overlays.remove(it) }
            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay?.outlinePaint?.color = Color.RED
            roadOverlay?.outlinePaint?.strokeWidth = 10f
            map!!.overlays.add(roadOverlay)

            Toast.makeText(this, "Distancia de la ruta: ${road.mLength} km", Toast.LENGTH_LONG).show()
        }
    }
    private fun removeRoute() {
        // Verifica si la variable roadOverlay no es null
        roadOverlay?.let {
            // Remueve la ruta del mapa
            map?.overlays?.remove(it)
            // Limpia la variable roadOverlay
            roadOverlay = null
        }
    }


    fun readJsonPairs(fileName: String): List<Pair<Double, Double>> {
        val inputStream = assets.open(fileName)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(jsonString)
        val locationsArray = json.getJSONArray("locations")
        val pairs = mutableListOf<Pair<Double, Double>>()

        for (i in 0 until locationsArray.length()) {
            val locationObject = locationsArray.getJSONObject(i)
            val latitude = locationObject.getDouble("latitude")
            val longitude = locationObject.getDouble("longitude")
            pairs.add(Pair(latitude, longitude))
        }

        return pairs
    }

}