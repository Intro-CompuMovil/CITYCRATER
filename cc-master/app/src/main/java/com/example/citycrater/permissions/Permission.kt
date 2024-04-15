package com.example.citycrater.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat

class Permission {

    companion object {

        const val MY_PERMISSION_REQUEST_CAMERA = 0
        const val MY_PERMISSION_REQUEST_GALLERY = 1
        const val IMAGE_PICKER_REQUEST = 2
        const val MY_PERMISSION_REQUEST_LOCATION = 3
        const val REQUEST_IMAGE_CAPTURE = 4

    }


}