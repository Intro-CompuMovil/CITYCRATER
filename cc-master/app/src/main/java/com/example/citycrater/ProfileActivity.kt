package com.example.citycrater

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //pedir permisos de camara y galeria
        val btnCamera = findViewById<ImageButton>(R.id.btnCamera)
        btnCamera.setOnClickListener { permisoCamara() }

        val btnGallery = findViewById<ImageButton>(R.id.btnGallery)
        btnGallery.setOnClickListener { permisoGaleria() }

    }

    fun permisoCamara(){

        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                //performAction(...)
                Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.CAMERA) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined.
                //showInContextUI(...)
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    Datos.MY_PERMISSION_REQUEST_CAMERA)
            }
            else -> {
                // You can directly ask for the permission.
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    Datos.MY_PERMISSION_REQUEST_CAMERA)
            }
        }


    }

    fun permisoGaleria(){

        val pickImage = Intent(Intent.ACTION_PICK)
        pickImage.type = "image/*"
        startActivityForResult(pickImage, Datos.IMAGE_PICKER_REQUEST)


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            Datos.IMAGE_PICKER_REQUEST ->{
                if(resultCode == Activity.RESULT_OK){
                    try {
                        //Logica de seleccion de imagen
                    } catch (e: FileNotFoundException){
                        e.printStackTrace()
                    }
                }
            }
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //val textV = findViewById<TextView>(R.id.textView)
        when (requestCode) {
            Datos.MY_PERMISSION_REQUEST_CAMERA -> {

                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()
                } else {
                    // Explain to the user that the feature is unavailable

                }
                return
            }
            else -> {
                // Ignore all other requests.

            }
        }
    }

}