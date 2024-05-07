package com.example.citycrater

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivityHomeBinding
import com.example.citycrater.databinding.ActivityReportFixedBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.markers.MarkerType
import com.example.citycrater.permissions.Permission
import com.example.citycrater.users.UserSessionManager
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.io.FileNotFoundException

class ReportFixedActivity : AppCompatActivity() {
    lateinit var binding: ActivityReportFixedBinding

    //MAPA
    private var map : MapView? = null
    private var currentLocationmarker: Marker? = null
    private lateinit var point: GeoPoint
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var key = ""
    private var size = ""

    //SENSORES
    private lateinit var mSensorManager: SensorManager
    private lateinit var mLightSensor: Sensor
    private lateinit var mLightSensorListener: SensorEventListener
    private var darkModeLum: Boolean = false
    private var lightModeLum: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportFixedBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().setUserAgentValue("com.example.citycrater")

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
        size = intent.getStringExtra("size")!!
        key = intent.getStringExtra("key")!!

        Toast.makeText(this, key, Toast.LENGTH_SHORT).show()

        val direccion = MapManager.getAddressFromCoordinates(latitude, longitude)
        point = GeoPoint(latitude, longitude)

        binding.location.text = longitude.toString() + "  ;  "  + latitude.toString()
        binding.size.text = size

        //image
        downloadFile()

        map!!.onResume()

        mSensorManager.registerListener(mLightSensorListener, mLightSensor,
            SensorManager.SENSOR_DELAY_NORMAL)

        val mapController = map!!.controller
        mapController.setZoom(15)
        createMarker(point,direccion, null, R.drawable.baseline_location_on_24, MarkerType.CURRENT)
        currentLocationmarker?.let { map!!.overlays.add(it) }
        mapController.setCenter(point);

    }

    private fun downloadFile() {
        val storage = FirebaseStorage.getInstance()
        val imageRef = storage.reference.child("${DataBase.PATH_BUMPS}/${key}/${DataBase.BUMP_REGISTERED_IMAGE_NAME}_${key}.jpg")

        val localFile = File.createTempFile("bumps_${key}", "jpg")
        imageRef.getFile(localFile)
            .addOnSuccessListener { taskSnapshot ->
                // Descarga exitosa del archivo
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                binding.imgBumpBefore.setImageBitmap(bitmap) // Establecer la imagen en el ImageView
                Log.i("FBApp", "Descargado exitosamente")
            }
            .addOnFailureListener { exception ->
                // Manejar la falla en la descarga
                Log.e("FBApp", "Error al descargar el archivo", exception)
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

        binding.btnCamera.setOnClickListener {
            permisoCamara()
        }

        binding.btnGallery.setOnClickListener {
            permisoGaleria()
        }
    }

    //FUNCIONES DE LAS IMAGENES
    fun permisoGaleria(){
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                selectPhoto()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                Toast.makeText(this, "La aplicación necesita acceso a la galería para seleccionar fotos", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    Permission.MY_PERMISSION_REQUEST_GALLERY
                )
            }
            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    Permission.MY_PERMISSION_REQUEST_GALLERY
                )
            }
        }
    }
    fun permisoCamara(){
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePic()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Necesita permiso de camara", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    Permission.MY_PERMISSION_REQUEST_CAMERA)
            }
            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    Permission.MY_PERMISSION_REQUEST_CAMERA)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            Permission.IMAGE_PICKER_REQUEST ->{
                if(resultCode == Activity.RESULT_OK){
                    try {
                        //Logica de seleccion de imagen
                        val selectedImageUri = data!!.data
                        if(data.data != null){
                            binding.imgBumpAfter.setImageURI(selectedImageUri)
                        }
                    } catch (e: FileNotFoundException){
                        e.printStackTrace()
                    }
                }
            }
            Permission.REQUEST_IMAGE_CAPTURE ->{
                if (requestCode == Permission.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    binding.imgBumpAfter.setImageBitmap(imageBitmap)

                    //guardar
                    val imageUri = saveImageToGallery(imageBitmap)
                    if (imageUri != null) {
                        Toast.makeText(this, "Imagen guardada en la galería", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Permission.MY_PERMISSION_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    takePic()
                    Toast.makeText(this, "Permiso de camara concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permiso de camara negado", Toast.LENGTH_SHORT).show()

                }
            }
            Permission.MY_PERMISSION_REQUEST_GALLERY -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectPhoto()
                    Toast.makeText(this, "Permiso de galería concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun takePic(){
        val permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        val takePictureIntent =  Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, Permission.REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "No hay una cámara disponible en este dispositivo", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay permiso de camara", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),
                Permission.MY_PERMISSION_REQUEST_CAMERA)
        }
    }

    fun selectPhoto () {
        val permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val pickImage = Intent(Intent.ACTION_PICK)
            pickImage.type = "image/*"
            startActivityForResult(pickImage, Permission.IMAGE_PICKER_REQUEST)
        } else {
            Toast.makeText(this, "No hay permiso de galeria", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                Permission.MY_PERMISSION_REQUEST_GALLERY)
        }
    }

    fun saveImageToGallery(bitmap: Bitmap): Uri? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Imagen_${System.currentTimeMillis()}")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera") // <-- Change this line

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            try {
                val outStream = resolver.openOutputStream(uri)
                if (outStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                }
                outStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return uri
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