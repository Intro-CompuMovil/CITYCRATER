package com.example.citycrater

import android.R
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.citycrater.databinding.ActivityHomeBinding
import com.example.citycrater.databinding.ActivityRequestsBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.permissions.Permission
import com.example.citycrater.users.UserSessionManager

class RequestsActivity : AppCompatActivity() {
    lateinit var binding: ActivityRequestsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //vista de admin
        if(UserSessionManager.setUserView(binding.btnReports)){
            binding.btnReports.setOnClickListener {
                val intent = Intent(this, RequestsActivity::class.java)
                startActivity(intent)
            }
        }

        //lista de reparados
        val adapter = ArrayAdapter(this, R.layout.simple_list_item_1, MapManager.buildAdapter(this))
        binding.reqList.adapter = adapter

        binding.reqList.setOnItemClickListener(object: AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent!!.getItemAtPosition(position) as String
                val coordinates = selectedItem.split(";")

                val intent = Intent(baseContext, DetailActivity::class.java)
                intent.putExtra("latitude", coordinates[0])
                intent.putExtra("longitude", coordinates[1])
                startActivity(intent)
            }
        })

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnReportBump.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, RegisterBumpActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay permiso de ubicacion", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }

        binding.btnMap.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay permiso de ubicacion", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }

        binding.btnFriends.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, FriendsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay permiso de ubicacion", Toast.LENGTH_SHORT).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }

    }
}