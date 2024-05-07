package com.example.citycrater

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivitySignUpBinding
import com.example.citycrater.model.User
import com.example.citycrater.users.UserSessionManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class SignUpActivity : AppCompatActivity(),  AdapterView.OnItemSelectedListener {
    lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "SIGNUP"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = Firebase.auth

        binding.userType.onItemSelectedListener = this

        binding.btnSign.setOnClickListener {
            signUp()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val user = parent?.selectedItem
        Toast.makeText(baseContext, "User type: "+ user, Toast.LENGTH_LONG).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun signUp(){
        if(validateForm() && UserSessionManager.validateEmail(binding.email.text.toString()) &&
            UserSessionManager.validatePassword(binding.txtPassword.text.toString())){

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
        }
    }

    private fun saveUserToDatabase(user: FirebaseUser) {
        val database = FirebaseDatabase.getInstance().reference
        var userRef: DatabaseReference

        userRef = database.child(DataBase.PATH_USERS).child(user.uid)

        val userToSave = User(binding.name.text.toString(),
            binding.email.text.toString(),
            binding.phone.text.toString(),
            binding.txtPassword.text.toString(),
            UserSessionManager.CURRENT)

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
            startActivity(Intent(this, HomeActivity::class.java))
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

        if(binding.userType.selectedItem.toString() == UserSessionManager.ADMIN){
            UserSessionManager.CURRENT = UserSessionManager.ADMIN
        }else if (binding.userType.selectedItem.toString() == UserSessionManager.DRIVER){
            UserSessionManager.CURRENT = UserSessionManager.DRIVER
        }else{
            Toast.makeText(this, "Selection required.", Toast.LENGTH_SHORT).show()
        }

        return valid
    }

}