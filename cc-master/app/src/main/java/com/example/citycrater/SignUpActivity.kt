package com.example.citycrater

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.UserManager
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import com.example.citycrater.databinding.ActivityHomeBinding
import com.example.citycrater.databinding.ActivitySignUpBinding
import com.example.citycrater.users.UserSessionManager

class SignUpActivity : AppCompatActivity(),  AdapterView.OnItemSelectedListener {
    lateinit var binding: ActivitySignUpBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.userType.onItemSelectedListener = this

        binding.btnSign.setOnClickListener {
            if(binding.userType.selectedItem.toString() == UserSessionManager.ADMIN){
                UserSessionManager.CURRENT = UserSessionManager.ADMIN
            }else if (binding.userType.selectedItem.toString() == UserSessionManager.DRIVER){
                UserSessionManager.CURRENT = UserSessionManager.DRIVER
            }
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val user = parent?.selectedItem
        Toast.makeText(baseContext, "User: "+ user, Toast.LENGTH_LONG).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

}