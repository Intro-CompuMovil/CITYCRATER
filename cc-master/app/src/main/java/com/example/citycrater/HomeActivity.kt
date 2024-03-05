package com.example.citycrater

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //pedir permisos de ubicacion

        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        val btnReportBump = findViewById<ImageButton>(R.id.btnReportBump)
        val btnMap = findViewById<ImageButton>(R.id.btnMap)
        val btnFriends = findViewById<ImageButton>(R.id.btnFriends)

        btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        btnReportBump.setOnClickListener {
            val intent = Intent(this, RegisterBumpActivity::class.java)
            startActivity(intent)
        }

        btnMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        btnFriends.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            startActivity(intent)
        }

    }
}