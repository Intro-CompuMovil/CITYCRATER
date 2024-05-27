package com.example.citycrater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.citycrater.database.DataBase
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
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
import java.io.File

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding

    //MAPA
    private var map : MapView? = null
    private var currentLocationmarker: Marker? = null
    private lateinit var point: GeoPoint
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var size: String = ""
    private var keyBump: String = ""
    private var keyReport: String = ""

    //SENSORES
    private lateinit var mSensorManager: SensorManager
    private lateinit var mLightSensor: Sensor
    private lateinit var mLightSensorListener: SensorEventListener
    private var darkModeLum: Boolean = false
    private var lightModeLum: Boolean = true

    //DATABASE
    private val TAG = "FB DELETE BUMP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().setUserAgentValue("com.example.citycrater")

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //INICIALIZACION
        initialize()

        //LISTENNERS DE LA PANTALLA
        setListenners()

        map!!.overlays.add(createOverlayEvents())
    }


    private fun downloadImage(imageName: String, imageView: ImageView) {
        val storage = FirebaseStorage.getInstance()
        val imageRef = storage.reference.child("${DataBase.PATH_BUMPS}/$keyBump/${imageName}_$keyBump.jpg")

        val localFile = File.createTempFile("bumps_$keyBump", "jpg")
        imageRef.getFile(localFile)
            .addOnSuccessListener { taskSnapshot ->
                // Descarga exitosa del archivo
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                imageView.setImageBitmap(bitmap) // Establecer la imagen en el ImageView
                Log.i("FBApp", "Descargado exitosamente")
            }
            .addOnFailureListener { exception ->
                // Manejar la falla en la descarga
                if (exception is StorageException && exception.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    Log.w("FBApp", "Imagen $imageName no encontrada", exception)
                } else {
                    // Manejar otros tipos de errores
                    Log.e("FBApp", "Error al descargar el archivo", exception)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        map!!.onResume()
        mSensorManager.registerListener(mLightSensorListener, mLightSensor,
            SensorManager.SENSOR_DELAY_NORMAL)

        //vista de admin
        if(UserSessionManager.setAdminReportsView(binding.btnReports)){
            binding.btnReports.setOnClickListener {
                val intent = Intent(this, RequestsActivity::class.java)
                startActivity(intent)
            }
        }

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

        val intent = this.intent
        latitude = intent.getStringExtra("latitude")!!.toDouble()
        longitude = intent.getStringExtra("longitude")!!.toDouble()
        size = intent.getStringExtra("size")!!
        keyBump = intent.getStringExtra("keyBump")!!
        keyReport = intent.getStringExtra("keyReport")!!

        // TEXT INFO
        binding.txtlocation.text = longitude.toString() + "  ;  "  + latitude
        binding.txtSize.text = size

        // LOCATION INFO
        val direccion = MapManager.getAddressFromCoordinates(latitude, longitude)
        point = GeoPoint(latitude, longitude)

        //IMAGES INFO
        downloadImage(DataBase.BUMP_FIXED_IMAGE_NAME, binding.imgBumpAfter)
        downloadImage(DataBase.BUMP_REGISTERED_IMAGE_NAME, binding.imgBumpBefore)

        val mapController = map!!.controller
        mapController.setZoom(15)
        createMarker(point,direccion, null, R.drawable.baseline_location_on_24, MarkerType.CURRENT)
        currentLocationmarker?.let { map!!.overlays.add(it) }
        mapController.setCenter(point);
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

        binding.btnDelete.setOnClickListener {
            deleteBump()
        }
    }

    private fun deleteBump(){
        //eliminar reporte del realtime
        val database = FirebaseDatabase.getInstance()
        var myRef = database.getReference(DataBase.PATH_FIXED_BUMPS + keyReport)

        myRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "ELIMINO REPORTE", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Successfully deleted report.")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed to delete report: $exception")
            }

        //eliminar hueco del realtime
        myRef = database.getReference(DataBase.PATH_BUMPS + keyBump)
        myRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "ELIMINO HUECO", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Successfully deleted bump.")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed to delete bump: $exception")
            }

        //eliminar imagenes
        val storage = FirebaseStorage.getInstance()
        var storageRef = storage.getReference(
            "${DataBase.PATH_BUMPS}/$keyBump/${DataBase.BUMP_REGISTERED_IMAGE_NAME}_$keyBump.jpg")

        //eliminar imagen de hueco
        storageRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "ELIMINO IAMGEN DE HUECO", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Successfully deleted bump image.")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed to delete bump image: $exception")
            }

        //eliminar imagen de hueco reparado
        storageRef = storage.getReference(
            "${DataBase.PATH_BUMPS}/$keyBump/${DataBase.BUMP_FIXED_IMAGE_NAME}_$keyBump.jpg")

        storageRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "ELIMINO IAMGEN DE HUECO REPARADO", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Successfully deleted fixed bump image.")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed to delete fixed bump image: $exception")
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