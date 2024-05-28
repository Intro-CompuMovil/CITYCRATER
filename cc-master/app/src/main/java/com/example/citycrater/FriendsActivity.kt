package com.example.citycrater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivityFriendsBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.markers.MarkerType
import com.example.citycrater.model.Bump
import com.example.citycrater.model.User
import com.example.citycrater.permissions.Permission
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
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay


class FriendsActivity : AppCompatActivity() {

    lateinit var binding: ActivityFriendsBinding

    //MAPA
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var map: MapView? = null
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private var currentLocationmarker: Marker? = null
    private var userMarker: Marker? = null
    private var radius: Double = 100000.0 //valor por defecto
    private var newRadius: Double = 100000.0 //valor por defecto
    private var previousRadius: Double = 100000.0 //valor por defecto
    private var circleOverlay: Polygon? = null
    private val markers = HashMap<String, Marker>()


    //SENSORES
    private lateinit var mSensorManager: SensorManager
    private lateinit var mLightSensor: Sensor
    private lateinit var mLightSensorListener: SensorEventListener
    private var darkModeLum: Boolean = false
    private var lightModeLum: Boolean = true


    //DATABASE
    private val database = FirebaseDatabase.getInstance()
    private lateinit var usersRef: DatabaseReference
    private lateinit var childEventListener: ChildEventListener
    val TAG = "FB UPDATE USER"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().setUserAgentValue("com.example.citycrater")

        binding = ActivityFriendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //INICIALIZACION DE VARIABLES
        initialize()

        //LISTENER BUSQUEDA (RADIO)
        listennerRadio()

        //LISTENERS DE LA PANTALLA
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

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

        listenForUsers()
    }

    private fun listenForUsers(){
        usersRef = database.getReference(DataBase.PATH_USERS)

        childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val user = dataSnapshot.getValue(User::class.java)
                if(currentLocationmarker != null && user != null &&
                    MapManager.isInsideRadious(radius, currentLocationmarker!!.position, GeoPoint(user.latitude, user.longitude))
                    && user.email != UserSessionManager.CURRENT.email){

                    userMarker = createMarkerRetMark(GeoPoint(user!!.latitude, user!!.longitude), "${user.name}", null, R.drawable.baseline_location_pin_25)
                    userMarker.let { map!!.overlays.add(it) }

                    // Store the marker in the HashMap
                    userMarker?.let { marker ->
                        dataSnapshot.key?.let { key ->
                            markers[key] = marker
                        }
                    }
                }

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val changedUser = dataSnapshot.getValue(User::class.java)
                if(currentLocationmarker != null && changedUser != null
                    && MapManager.isInsideRadious(radius, currentLocationmarker!!.position, GeoPoint(changedUser.latitude, changedUser.longitude))
                    && changedUser.email != UserSessionManager.CURRENT.email){
                    // Get the existing marker from the HashMap
                    dataSnapshot.key?.let { key ->
                        val marker = markers[key]
                        if (marker != null) {
                            // Update the marker's position
                            marker.position = GeoPoint(changedUser.latitude, changedUser.longitude)
                            marker.title = "${changedUser.name}"

                            // Update the map
                            map!!.invalidate()
                        }
                    }
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val removedBump = dataSnapshot.getValue(User::class.java)
                if (removedBump != null) {
                    Log.d(TAG, "Removed bump: ${removedBump.latitude}, ${removedBump.longitude}, ${removedBump.name}")
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // This method is triggered when a child location's priority changes
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Failed to read value: $databaseError")
            }
        }

        usersRef.addChildEventListener(childEventListener)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        map!!.onPause()
        mSensorManager.unregisterListener(mLightSensorListener)
        usersRef.removeEventListener(childEventListener)
    }
    private fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }
    private fun initialize() {
        map = binding.osmMap
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.setMultiTouchControls(true)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        darkModeLum = false
        lightModeLum = true

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()
    }
    private fun createLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

    //LISTENNERS DEL RADIO
    private fun listennerRadio() {
        binding.radio.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val radioE: Double? = binding.radio.text.toString().toDouble()
                if (radioE != null) {
                    //actualizar radio
                    previousRadius = radius
                    updateDisplayedUsers(radioE)
                } else {
                    Toast.makeText(this, "Radio vacío", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    private fun updateDisplayedUsers(radioE: Double) {
        if (radioE > previousRadius) {
            // Verificar la base de datos y agregar nuevos usuarios dentro del nuevo radio
            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (userSnapshot in dataSnapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null && currentLocationmarker != null &&
                            !markers.containsKey(userSnapshot.key) &&
                            MapManager.isInsideRadious(radioE, currentLocationmarker!!.position, GeoPoint(user.latitude, user.longitude))
                            && user.email != UserSessionManager.CURRENT.email) {

                            val userMarker = createMarkerRetMark(GeoPoint(user.latitude, user.longitude), "${user.name}", null, R.drawable.baseline_location_pin_25)
                            userMarker?.let {
                                map?.overlays?.add(it)
                                markers[userSnapshot.key!!] = it
                            }
                        }
                    }
                    map!!.invalidate()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "Failed to read value: $databaseError")
                }
            })
        } else if (radioE < previousRadius) {
            // Verificar el HashMap y eliminar usuarios que estén fuera del nuevo radio
            val iterator = markers.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val userId = entry.key
                val marker = entry.value
                if (!MapManager.isInsideRadious(radioE, currentLocationmarker!!.position, marker.position)) {
                    // El usuario está fuera del nuevo radio, eliminar el marcador del mapa y del HashMap
                    map?.overlays?.remove(marker)
                    iterator.remove()
                }
            }
            map?.invalidate()
        }
        // Actualizar el valor de previousRadius al nuevo radio
        radius = radioE
        removeCircle()
        drawCircle()
    }

    //LISTENNERS DE LA PANTALLA
    private fun setListenners() {
        mLightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event!!.values[0] < 12) {
                    darkModeLum = true
                    map!!.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)

                } else {
                    lightModeLum = true
                    map!!.overlayManager.tilesOverlay.setColorFilter(null)
                }
                if (lightModeLum && darkModeLum) {
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
                    removeCircle()
                    val point = GeoPoint(location.latitude, location.longitude)
                    createMarker(
                        point,
                        "you",
                        null,
                        R.drawable.baseline_location_pin_24,
                        MarkerType.CURRENT
                    )

                    // Update user's location in Firebase

                    currentLocationmarker?.let { map!!.overlays.add(it) }
                    //map!!.controller.setCenter(currentLocationmarker!!.position)
                    drawCircle()
                }
            }
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

    //FUNCIONES RELACIONADAS AL MAPA
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
            if (currentLocationmarker == null) {
                currentLocationmarker = Marker(map)
            }
            title?.let { currentLocationmarker!!.title = it }
            desc?.let { currentLocationmarker!!.subDescription = it }
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, this.theme)
                currentLocationmarker!!.icon = myIcon
            }
            currentLocationmarker!!.position = p
            currentLocationmarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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

    private fun drawCircle(){
        // Crea una instancia de Polygon
        val circle = Polygon(map)
        circleOverlay = circle
        circleOverlay?.setInfoWindow(null)

        // Configura el centro del círculo
        circle.points = Polygon.pointsAsCircle(currentLocationmarker!!.position, radius)

        // Configura el color del relleno (transparente) y el color del borde
        circle.fillPaint.color = Color.argb(50, 0, 0, 255) // Color azul con transparencia
        circle.fillPaint.style = Paint.Style.FILL

        circle.outlinePaint.color = Color.BLUE // Color del borde
        circle.outlinePaint.strokeWidth = 2f // Ancho del borde

        // Agrega el círculo al mapa
        map?.overlays?.add(circle)
        map?.invalidate() // Actualiza el mapa para mostrar el círculo
    }

    private fun removeCircle() {
        // Verifica si circleOverlay no es nulo
        if (circleOverlay != null) {
            // Remueve el círculo del mapa
            map?.overlays?.remove(circleOverlay)
            // Limpia la referencia
            circleOverlay = null
            // Actualiza el mapa
            map?.invalidate()
        }
    }
}