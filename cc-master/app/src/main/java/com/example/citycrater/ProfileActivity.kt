package com.example.citycrater

import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.citycrater.databinding.ActivityProfileBinding
import com.example.citycrater.databinding.ActivityRegisterBumpBinding
import com.example.citycrater.model.User
import com.example.citycrater.permissions.Permission
import com.example.citycrater.users.UserSessionManager
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private  var auth: FirebaseAuth = Firebase.auth

    lateinit var binding: ActivityProfileBinding

    private var photoUri: Uri? = null

    private val database = FirebaseDatabase.getInstance()

    private lateinit var myRef: DatabaseReference

    private val storage = Firebase.storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //LISTENNERS DE LA PANTALLA
        setlistenners()

        //CAMPOS DE LA PANTALLA
        setFields()

    }

    //LISTENNERS DE LA PANTALLA
    fun setlistenners (){

        binding.btnCamera.setOnClickListener {
            permisoCamara()
        }

        binding.btnGallery.setOnClickListener {
            permisoGaleria()
        }
        binding.btnLogout.setOnClickListener(){
            logOut()
        }
        binding.btnUpdateUser.setOnClickListener(){
            //saveuser
            updateUser()
        }
    }

    fun setFields (){

        val db = database.getReference("users/${auth.currentUser!!.uid}")
        var userObject: User? = null
        Log.i("PATH", "users/${auth.currentUser!!.uid}")

        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userObject = dataSnapshot.getValue(User::class.java)
                binding.name.hint = userObject!!.name
                binding.email.hint = userObject!!.email
                binding.phone.hint = userObject!!.phone

                if(userObject!!.photo.isNotEmpty()){
                    downloadFile()
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
                Log.w(TAG, "loadUser:onCancelled", databaseError.toException())
            }
        })

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
                            binding.imgProfile.setImageURI(selectedImageUri)
                            //guardar

                            if (selectedImageUri != null) {
                                photoUri = selectedImageUri
                            }


                        }
                    } catch (e: FileNotFoundException){
                        e.printStackTrace()
                    }
                }
            }
            Permission.REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Load the full-quality image into ImageView
                    binding.imgProfile.setImageURI(photoUri)
                    val imageUri = saveImageToGallery(MediaStore.Images.Media.getBitmap(contentResolver, photoUri))
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

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private fun uploadFileFromUri(imgUri: Uri) {
        val file = imgUri
        val imageRef = storage.reference.child("users/profile/${auth.currentUser!!.uid}/image.jpg")
        imageRef.putFile(file)
            .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {
                override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot) {
// Get a URL to the uploaded content
                    Log.i("FBApp", "Successfully uploaded image")
                }
            })
            .addOnFailureListener(object : OnFailureListener {
                override fun onFailure(exception: Exception) {
// Handle unsuccessful uploads
// ...
                }
            })
    }


    fun logOut(){
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    fun updateUser(){
        //Agregar datos a realtime Database
        var userObject : User? = User()
        myRef = database.getReference("users/${auth.currentUser!!.uid}")
        val db = database.getReference("users/${auth.currentUser!!.uid}")
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userObject = dataSnapshot.getValue(User::class.java)
                if(photoUri != null){
                    uploadFileFromUri(photoUri!!)
                    userObject!!.photo = "users/profile/${auth.currentUser!!.uid}/image.jpg"
                }
                if(binding.name.text.isNotEmpty()){
                    userObject!!.name = binding.name.text.toString()
                }
                if(binding.phone.text.isNotEmpty()){
                    userObject!!.phone = binding.phone.text.toString()
                }
                if(binding.email.text.isNotEmpty()){
                    if(UserSessionManager.validateEmail(binding.email.text.toString())){
                        auth.currentUser!!.updateEmail(binding.email.text.toString())
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    userObject!!.email = binding.email.text.toString()
                                    Log.d(TAG, "User email address updated.")
                                }
                            }
                    }else{
                        Toast.makeText(baseContext, "Correo invalido", Toast.LENGTH_SHORT).show()
                    }
                }
                if(binding.txtPassword.text.isNotEmpty()){
                    if(UserSessionManager.validatePassword(binding.txtPassword.text.toString())){
                        auth.currentUser!!.updatePassword(binding.txtPassword.text.toString())
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    userObject!!.password = binding.txtPassword.text.toString()
                                    Log.d(TAG, "User password updated.")
                                }
                            }
                    }
                }
                //LOGICA CAMBIO EMAIL Y CONTRASEÑA
                Log.i("LONGNAME", binding.name.text.isNotEmpty().toString())
                Log.i("LONGEMAIL", binding.email.text.isEmpty().toString())

                myRef.setValue(userObject)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
                Log.w(TAG, "loadUser:onCancelled", databaseError.toException())
            }
        })

    }

    private fun downloadFile() {

        val localFile = File.createTempFile("images", "jpg")
        val imageRef = storage.reference.child("users/profile/${auth.currentUser!!.uid}/image.jpg")
        imageRef.getFile(localFile)
            .addOnSuccessListener { taskSnapshot ->
// Successfully downloaded data to local file
//...
                Log.i("FBApp", "succesfully downloaded")
                binding.imgProfile.setImageURI(Uri.fromFile(localFile))
// Update UI using the localFile
            }.addOnFailureListener { exception ->
// Handle failed download
// ...
            }
    }
    private fun isEmailValid(email: String): Boolean {
        if (!email.contains("@") ||
            !email.contains(".") ||
            email.length < 5)
            return false
        return true
    }

    fun validatePassword(password: String): Boolean{
        if (password.length < 8)
            return false
        return true
    }




}