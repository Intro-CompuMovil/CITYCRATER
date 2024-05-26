package com.example.citycrater

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.Toast
import android.util.Log
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivityLoginBinding
import com.example.citycrater.databinding.ActivitySignUpBinding
import com.example.citycrater.model.User
import com.example.citycrater.users.UserSessionManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LOGIN"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()

        binding.btnLog.setOnClickListener {
            signInUser(binding.txtUsername.text.toString(), binding.txtPassword.text.toString())
        }
    }

    private fun signInUser(email: String, password: String){
        if(validateForm()){
            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI
                        Log.d(TAG, "signInWithEmail:success:")
                        updateUI(auth.currentUser)
                    } else {
                        Log.e(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        if (TextUtils.isEmpty(binding.txtUsername.text.toString())) {
            binding.txtUsername.error = "Required."
            valid = false
        } else {
            binding.txtUsername.error = null
        }
        if (TextUtils.isEmpty(binding.txtPassword.text.toString())) {
            binding.txtPassword.error = "Required."
            valid = false
        } else {
            binding.txtPassword.error = null
        }
        return valid
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
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
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
}