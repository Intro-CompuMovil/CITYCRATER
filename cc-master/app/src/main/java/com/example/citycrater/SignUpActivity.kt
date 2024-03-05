package com.example.citycrater

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast

class SignUpActivity : AppCompatActivity(),  AdapterView.OnItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val btnSign = findViewById<Button>(R.id.btnSign)
        val spinner = findViewById<Spinner>(R.id.userType)

        spinner.onItemSelectedListener = this

        btnSign.setOnClickListener {
            if(spinner.selectedItem.toString().equals("Driver")){
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
            }else if (spinner.selectedItem.toString().equals("Admin")){
                val intent = Intent(this, HomeAdminActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val user = parent?.selectedItem
        Toast.makeText(baseContext, "User: "+ user, Toast.LENGTH_LONG).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

}