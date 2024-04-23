package com.example.citycrater

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.util.Log
import com.example.citycrater.databinding.ActivityLoginBinding
import com.example.citycrater.databinding.ActivitySignUpBinding
import com.example.citycrater.users.UserSessionManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //LISTENNERS
        setListenners()

        //AUTENTICAR
        auth = Firebase.auth
        UserSessionManager.CURRENT = UserSessionManager.ADMIN

    }


    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun setListenners(){
        binding.btnLog.setOnClickListener {
            //AUTENTICAR
            signInUser(binding.username.text.toString(), binding.password.text.toString())
        }
    }

    private fun signInUser(email: String, password: String){
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI
                    Log.i("SIGNIN", "signInWithEmail:success:")
                    updateUI(auth.currentUser)
                } else {
                    Log.i("SIGNIN", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            Toast.makeText(this, "no existe user", Toast.LENGTH_SHORT).show()
        }
    }
}