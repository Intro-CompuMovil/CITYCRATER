package com.example.citycrater

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivityMapBinding
import com.example.citycrater.databinding.ActivityRegisterBumpBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.markers.MarkerType
import com.example.citycrater.model.Bump
import com.example.citycrater.permissions.Permission
import com.example.citycrater.users.UserSessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
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
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterBumpActivity : AppCompatActivity() {

    lateinit var binding: ActivityRegisterBumpBinding

    //MAPA
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var map : MapView? = null
    private var currentLocationmarker: Marker? = null

    //SENSORES
    private lateinit var mSensorManager: SensorManager
    private lateinit var mLightSensor: Sensor
    private lateinit var mLightSensorListener: SensorEventListener
    private var darkModeLum: Boolean = false
    private var lightModeLum: Boolean = true

    //PHOTO
    private lateinit var photoUri: Uri
    var localPhoto: Uri? = null
    var url: String = ""

    //DATABASE
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    val TAG = "REGISTER_BUMP"
    val TAG_LOCATION = "LOCATION"

    //GEOCODER
    var mGeocoder: Geocoder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().setUserAgentValue("com.example.citycrater")

        binding = ActivityRegisterBumpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //vista de admin
        if(UserSessionManager.setAdminReportsView(binding.btnReports)){
            binding.btnReports.setOnClickListener {
                val intent = Intent(this, RequestsActivity::class.java)
                startActivity(intent)
            }
        }

        //INICIALIZACIÓN DE VARIABLES
        initialize()

        //LISTENNER DE BUSQUEDA DE UBICACION
        listennerGeocoder()

        //LISTENNERS DE LA PANTALLA
        setListenners()

        map!!.overlays.add(createOverlayEvents())
    }

    override fun onResume() {
        super.onResume()
        map!!.onResume()

        mSensorManager.registerListener(mLightSensorListener, mLightSensor,
            SensorManager.SENSOR_DELAY_NORMAL)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                Log.i(TAG_LOCATION, "onSuccess location")
                if (location != null) {
                    Log.i("LOCATION", "Longitud: " + location.longitude)
                    Log.i("LOCATION", "Latitud: " + location.latitude)
                    val mapController = map!!.controller
                    mapController.setZoom(15)
                    val startPoint = GeoPoint(location.latitude, location.longitude);
                    mapController.setCenter(startPoint);
                } else {
                    Log.i(TAG_LOCATION, "FAIL location")
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()
        map!!.onPause()
        mSensorManager.unregisterListener(mLightSensorListener)
    }

    //INICIALIZACIÓN DE VARIABLES
    private fun initialize(){
        mGeocoder = Geocoder(this)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        darkModeLum = false
        lightModeLum = true

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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

        binding.btnCamera.setOnClickListener {
            permisoCamara()
        }

        binding.btnGallery.setOnClickListener {
            permisoGaleria()
        }

        binding.btnRegisterBump.setOnClickListener {
            registerBump()
        }
    }

    private fun registerBump() {
        if (validaetForm()) {
            val newBump = Bump(currentLocationmarker!!.position.latitude,
                currentLocationmarker!!.position.longitude,
                binding.sizeSpinner.selectedItem.toString())

            myRef = database.getReference(DataBase.PATH_BUMPS)

            // Query the database for Bumps with the same latitude and longitude
            val query = myRef.orderByChild("latitude").equalTo(newBump.latitude)
                .limitToFirst(1)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var exists = false

                    for (snapshot in dataSnapshot.children) {
                        val bump = snapshot.getValue(Bump::class.java)
                        if (bump != null && bump.longitude == newBump.longitude) {
                            exists = true
                            break
                        }
                    }

                    if (!exists) {
                        // If no Bump with the same latitude and longitude exists, register the new Bump
                        val key = myRef.push().key
                        myRef = database.getReference(DataBase.PATH_BUMPS + key)

                        myRef.setValue(newBump)
                            .addOnSuccessListener {
                                saveImage(key)
                                Log.d(TAG, "BUMP SUCCESSFULLY REGISTERED IN REALTIME")
                            }
                            .addOnFailureListener { exception ->
                                Log.w(TAG, "Failed to save bump: $exception")
                            }
                    } else {
                        Toast.makeText(baseContext, "A bump with the same location already exists", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "Failed to read value: $databaseError")
                }
            })
        } else {
            Toast.makeText(this, "Complete data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage(key: String?){
        if(key != null){
            //referencia de la imagen en la bd
            val storageRef = FirebaseStorage.getInstance().reference
            val imagesRef = storageRef.child(DataBase.PATH_BUMPS + key)

            // Crea una referencia única para la imagen (por ejemplo, usando el timestamp actual)
            val imageName = DataBase.BUMP_REGISTERED_IMAGE_NAME + "_${key}.jpg"
            val imageRef = imagesRef.child(imageName)

            // Sube la imagen al Firebase Storage
            imageRef.putFile(localPhoto!!).addOnSuccessListener(object :
                OnSuccessListener<UploadTask.TaskSnapshot> {
                override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot) {
                    Log.d(TAG, "Successfully uploaded image, REGISTER OF BUMP COMPLETED")
                    Toast.makeText(baseContext, "Register bump correct", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(baseContext, MapActivity::class.java))
                }
            }).addOnFailureListener(object : OnFailureListener {
                override fun onFailure(exception: Exception) {
                    // Handle unsuccessful uploads
                    Log.w(TAG, "Failed uploading image: $exception")
                }
            })
        }else{
            Log.d(TAG, "KEY OF BUMP NULL")
        }
    }

    private fun validaetForm(): Boolean{
        var valid = true

        if(currentLocationmarker == null){
            valid = false
        }
        if(TextUtils.isEmpty(binding.sizeSpinner.selectedItem.toString())){
            valid = false
        }
        if(localPhoto == null){
            valid = false
        }

        return valid
    }

    //LISTENNER DE BUSQUEDA DE UBICACION
    private fun listennerGeocoder (){
        binding.location.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val addressString = binding.location.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        if (map != null && mGeocoder != null) {
                            val addresses = mGeocoder!!.getFromLocationName(addressString, 1)
                            if (addresses != null && addresses.isNotEmpty()) {
                                val addressResult = addresses[0]
                                val position = GeoPoint(addressResult.latitude, addressResult.longitude)

                                Log.i(TAG_LOCATION, "Dirección encontrada: ${addressResult.getAddressLine(0)}")
                                Log.i(TAG_LOCATION, "Latitud: ${addressResult.latitude}, Longitud: ${addressResult.longitude}")
                                binding.location.hint = addressResult.getAddressLine(0)

                                //Agregar Marcador al mapa
                                createMarker(position, addressString, null,
                                    R.drawable.baseline_location_on_24, MarkerType.ORIGIN)
                                currentLocationmarker?.let { map!!.overlays.add(it) }
                                map!!.controller.setCenter(currentLocationmarker!!.position)

                            } else {
                                Log.i(TAG_LOCATION, "Dirección no encontrada:" + addressString)
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

    //FUNCIONES RELACIONADAS CON EL MAPA
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
    private fun longPressOnMap(p: GeoPoint) {
        val address = MapManager.getAddressFromCoordinates(p.latitude, p.longitude)
        createMarker(p, address, null, R.drawable.baseline_location_on_24, MarkerType.DESTINATION)
        currentLocationmarker?.let { map!!.overlays.add(it) }
        map!!.controller.setCenter(currentLocationmarker!!.position)
        binding.location.setText(address)
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
    private fun takePic() {
        val permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    null
                }
                photoFile?.also {
                    photoUri = FileProvider.getUriForFile(
                        this,
                        "com.example.citycrater.FileProvider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, Permission.REQUEST_IMAGE_CAPTURE)
                }
            } else {
                Toast.makeText(this, "No hay una cámara disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay permiso de cámara", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), Permission.MY_PERMISSION_REQUEST_CAMERA)
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            DataBase.BUMP_REGISTERED_IMAGE_NAME, /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            Permission.IMAGE_PICKER_REQUEST ->{
                if(resultCode == Activity.RESULT_OK){
                    try {
                        //Logica de seleccion de imagen
                        val selectedImageUri = data!!.data
                        if(data.data != null){
                            binding.imgBump.setImageURI(selectedImageUri)
                            localPhoto = selectedImageUri
                        }
                    } catch (e: FileNotFoundException){
                        e.printStackTrace()
                    }
                }
            }
            Permission.REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Load the full-quality image into ImageView
                    localPhoto = photoUri
                    binding.imgBump.setImageURI(photoUri)
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

}