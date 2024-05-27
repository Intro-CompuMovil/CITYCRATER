package com.example.citycrater

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivitySignUpBinding
import com.example.citycrater.markers.MarkerType
import com.example.citycrater.model.User
import com.example.citycrater.permissions.Permission
import com.example.citycrater.users.UserSessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.util.GeoPoint


class SignUpActivity : AppCompatActivity(),  AdapterView.OnItemSelectedListener {
    lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "SIGNUP"

    //LOCATION
    lateinit var point: GeoPoint
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = Firebase.auth

        binding.userType.onItemSelectedListener = this

        permisoUbicacion()

        initialize()

        binding.btnSign.setOnClickListener {
            signUp()
        }

    }

    override fun onResume() {
        super.onResume()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                Log.i("LOCATION", "onSuccess location")
                if (location != null) {
                    Log.i("LOCATION", "Longitud: " + location.longitude)
                    Log.i("LOCATION", "Latitud: " + location.latitude)
                    point = GeoPoint(location.latitude, location.longitude);
                } else {
                    Log.i("LOCATION", "FAIL location")
                }
            }
        }

    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    fun permisoUbicacion(){
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                Toast.makeText(this, "Hay permiso de ubicacion", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, "Requiere acceso a ubicación para crear cuenta", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Permission.MY_PERMISSION_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Requiere permiso de ubicacion para crear cuenta", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }
    private fun initialize(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                Log.i("LOCATION", "Location update in the callback: $location")
                if (location != null) {
                    point = GeoPoint(location.latitude, location.longitude)
                }
            }
        }
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

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val user = parent?.selectedItem
        Toast.makeText(baseContext, "User type: "+ user, Toast.LENGTH_LONG).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun signUp(){
        if(validateForm() && UserSessionManager.validateEmail(binding.email.text.toString()) &&
            UserSessionManager.validatePassword(binding.txtPassword.text.toString())
            && point != null){

            auth.createUserWithEmailAndPassword(binding.email.text.toString(), binding.txtPassword.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)
                        val user = auth.currentUser
                        if (user != null) {
                            saveUserToDatabase(user)
                        }
                    } else {
                        Toast.makeText(this, "createUserWithEmail:Failure: " + task.exception.toString(),
                            Toast.LENGTH_SHORT).show()
                        task.exception?.message?.let { Log.e(TAG, it) }
                    }
                }
        }else{
            Toast.makeText(this, "Incorrect fields.", Toast.LENGTH_SHORT).show()
            Log.i("VALIDATE", validateForm().toString())
        }
    }

    private fun saveUserToDatabase(user: FirebaseUser) {
        val database = FirebaseDatabase.getInstance().reference
        var userRef: DatabaseReference

        userRef = database.child(DataBase.PATH_USERS).child(user.uid)

        val userToSave = User(binding.name.text.toString(),
            binding.email.text.toString(),
            binding.phone.text.toString(),
            binding.userType.selectedItem.toString(),
            point.latitude,
            point.longitude)

        userRef.setValue(userToSave)
            .addOnSuccessListener {
                Log.d(TAG, "User saved to database successfully!")
                updateUI(user)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed to save user to database: $exception")
                Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference(DataBase.PATH_USERS).child(currentUser.uid)

            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            UserSessionManager.CURRENT = user
                            UserSessionManager.CURRENT_UID = currentUser.uid
                            val intent = Intent(this@SignUpActivity, HomeActivity::class.java)
                            startActivity(intent)
                        }
                    } else {
                        Log.w(TAG, "User data not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Failed to read user data", error.toException())
                }
            })
        }
    }
    private fun validateForm(): Boolean{
        var valid = true

        if (TextUtils.isEmpty(binding.name.text.toString())) {
            binding.name.error = "Required."
            valid = false
        } else {
            binding.name.error = null
        }

        if (TextUtils.isEmpty(binding.phone.text.toString())) {
            binding.phone.error = "Required."
            valid = false
        } else {
            binding.phone.error = null
        }

        if (TextUtils.isEmpty(binding.email.text.toString())) {
            binding.email.error = "Required."
            valid = false
        } else {
            binding.email.error = null
        }

        if (TextUtils.isEmpty(binding.txtPassword.text.toString())) {
            binding.txtPassword.error = "Required."
            valid = false
        } else {
            binding.txtPassword.error = null
        }

        return valid
    }

}