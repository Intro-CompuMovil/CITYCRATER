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
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivityMapBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.markers.MarkerType
import com.example.citycrater.model.Bump
import com.example.citycrater.model.User
import com.example.citycrater.users.UserSessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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
import kotlin.random.Random
import kotlin.math.*

class MapActivity : AppCompatActivity() {

    lateinit var binding: ActivityMapBinding

    //MAPA
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var map : MapView? = null
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private var currentLocationmarker: Marker? = null
    private val markers = HashMap<String, Marker>()
    private var locationUpd: Boolean = false

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

    //DATABASE
    private val database = FirebaseDatabase.getInstance()
    private lateinit var bumpsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var childEventListener: ChildEventListener

    val TAG = "REGISTER_BUMP"
    val TAG_LOCATION = "LOCATION"

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

        //vista de admin
        if(UserSessionManager.setAdminReportsView(binding.btnReports)){
            binding.btnReports.setOnClickListener {
                val intent = Intent(this, RequestsActivity::class.java)
                startActivity(intent)
            }
        }

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

        //READ FROM DB ANT LISTEN
        listenForBumps()
    }

    private fun listenForBumps() {
        //bumpsRef = database.getReference(DataBase.PATH_BUMPS)
        bumpsRef = database.getReference(DataBase.PATH_BUMPS)

        childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                    val newBump = dataSnapshot.getValue(Bump::class.java)
                    if (newBump != null) {
                        Log.d(
                            TAG,
                            "New bump: ${newBump.latitude}, ${newBump.longitude}, ${newBump.size}"
                        )
                        // TODO: Update your UI here with the new bump
                        bumpLocationMarker = createMarkerRetMark(
                            GeoPoint(newBump.latitude, newBump.longitude),
                            "Size: ${newBump.size}",
                            null,
                            R.drawable.baseline_location_pin_25
                        )
                        bumpLocationMarker.let { map!!.overlays.add(it) }

                        // Create local variables for the latitude, longitude, and key
                        val latitude = newBump.latitude.toString()
                        val longitude = newBump.longitude.toString()
                        val size = newBump.size
                        val key = dataSnapshot.key

                        bumpLocationMarker?.setOnMarkerClickListener { marker, mapView ->
                            val intent = Intent(baseContext, ReportFixedActivity::class.java)
                            intent.putExtra("latitude", latitude)
                            intent.putExtra("longitude", longitude)
                            intent.putExtra("size", size)
                            intent.putExtra("key", key)
                            startActivity(intent)
                            true
                        }

                        // Store the marker in the HashMap
                        bumpLocationMarker?.let { marker ->
                            dataSnapshot.key?.let { key ->
                                markers[key] = marker
                            }
                        }
                    }
            }


            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val changedBump = dataSnapshot.getValue(Bump::class.java)
                if (changedBump != null) {
                    Log.d(TAG, "Changed bump: ${changedBump.latitude}, ${changedBump.longitude}, ${changedBump.size}")
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val removedBump = dataSnapshot.getValue(Bump::class.java)
                if (removedBump != null) {
                    Log.d(TAG, "Removed bump: ${removedBump.latitude}, ${removedBump.longitude}, ${removedBump.size}")
                    // TODO: Update your UI here to remove the bump
                    // Remove the marker from the map and the HashMap
                    dataSnapshot.key?.let { key ->
                        markers[key]?.let { marker ->
                            map!!.overlays.remove(marker)
                            markers.remove(key)
                        }
                    }
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // This method is triggered when a child location's priority changes
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Failed to read value: $databaseError")
            }
        }
        bumpsRef.addChildEventListener(childEventListener)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        map!!.onPause()
        mSensorManager.unregisterListener(mLightSensorListener)
        bumpsRef.removeEventListener(childEventListener)
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
                locationUpd = true
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    createMarker(point, "you", null, R.drawable.baseline_location_pin_24, MarkerType.CURRENT)
                    currentLocationmarker?.let { map!!.overlays.add(it) }
                    //map!!.controller.setCenter(currentLocationmarker!!.position)

                    // Update user's location in Firebase
                    val userLocationRef = FirebaseDatabase.getInstance().getReference("${DataBase.PATH_USERS}/${UserSessionManager.CURRENT_UID}")
                    val userLocation = mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude
                    )
                    //userLocationRef.updateChildren(userLocation)


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
            drawRouteWithoutBumps(startPoint,destinationPoint)
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



    private fun drawRouteWithoutBumps(start: GeoPoint, finish: GeoPoint) {
        var routePoints = ArrayList<GeoPoint>()
        var routePointsForRoute = ArrayList<GeoPoint>()
        var routeWithBump: Boolean
        routePoints.add(start)
        routePoints.add(finish)

        //routePoints.add(GeoPoint(4.751933, -74.048018))

        var roadAux = roadManager.getRoad(routePoints)

        do {
            var routeWithBump = false

            // Iterate over markers.values to access each Marker object
            for (markerEntry in markers.entries) {
                val markerPosition = markerEntry.value.position

                for (i in 0 until roadAux.mNodes.size) {
                    val nodeLocation = roadAux.mNodes[i].mLocation

                    // Check for proximity between the marker and the route nodes, excluding start and finish
                    if (bumpProximityDetector(markerPosition, nodeLocation)
                        && !bumpProximityDetector(finish, nodeLocation)
                        && !bumpProximityDetector(start, nodeLocation)) {

                        Log.i("DISTANCE BUMP: ${markerEntry.key} Route: $i", calculateDistance(markerPosition, nodeLocation).toString())
                        routeWithBump = true

                        Log.i("OLDPOINT", nodeLocation.toString())

                        val newPoint = generateRandomPoint(nodeLocation, 1.0)
                        Log.i("NEWPOINT", newPoint.toString())

                        routePoints.clear()
                        routePoints.add(start)

                        // Add the nodes up to the point where the bump was detected
                        for (k in 0 until i) {
                            routePoints.add(roadAux.mNodes[k].mLocation)
                        }
                        routePoints.add(newPoint)
                        routePoints.add(finish)
                        break
                    }
                }

                if (routeWithBump) {
                    roadAux = roadManager.getRoad(routePoints)
                    break
                }
            }
        } while (routeWithBump)


        Log.i("OSM_acticity", "Route length: ${roadAux.mLength} klm")
        Log.i("OSM_acticity", "Duration: ${roadAux.mDuration / 60} min")
        if (map != null) {
            roadOverlay?.let { map!!.overlays.remove(it) }
            roadOverlay = RoadManager.buildRoadOverlay(roadAux)
            roadOverlay?.outlinePaint?.color = Color.RED
            roadOverlay?.outlinePaint?.strokeWidth = 10f
            map!!.overlays.add(roadOverlay)

            Toast.makeText(this, "Distancia de la ruta: ${roadAux.mLength} km", Toast.LENGTH_LONG).show()
        }
    }

    fun bumpProximityDetector(point1: GeoPoint, point2: GeoPoint): Boolean {
        if(calculateDistance(point1, point2) <= 1){
            return true
        }
        return false
    }

    fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val earthRadius = 6371.0 // radius in kilometers

        val latDiff = Math.toRadians(point2.latitude - point1.latitude)
        val lonDiff = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(latDiff / 2).pow(2.0) +
                cos(Math.toRadians(point1.latitude)) * cos(Math.toRadians(point2.latitude)) *
                sin(lonDiff / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun generateRandomPoint(initialPoint: GeoPoint, distance: Double): GeoPoint {
        val radius = 6371.0 // Earth's radius in kilometers
        val bearing = Random.nextDouble(2 * PI) // Random direction
        val angularDistance = distance / radius // The angular distance

        val lat1 = Math.toRadians(initialPoint.latitude)
        val lon1 = Math.toRadians(initialPoint.longitude)

        val lat2 = asin(sin(lat1) * cos(angularDistance) +
                cos(lat1) * sin(angularDistance) * cos(bearing))

        var lon2 = lon1 + atan2(sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2))

        lon2 = (lon2 + 3 * PI) % (2 * PI) - PI // Normalize to -180..+180

        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

}