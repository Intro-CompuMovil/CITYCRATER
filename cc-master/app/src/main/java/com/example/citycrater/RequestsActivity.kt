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
import com.example.citycrater.database.DataBase
import com.example.citycrater.databinding.ActivityHomeBinding
import com.example.citycrater.databinding.ActivityRequestsBinding
import com.example.citycrater.mapsUtils.MapManager
import com.example.citycrater.model.FixedBump
import com.example.citycrater.permissions.Permission
import com.example.citycrater.users.UserSessionManager
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class RequestsActivity : AppCompatActivity() {
    lateinit var binding: ActivityRequestsBinding
    //DATABASE
    val database = FirebaseDatabase.getInstance()
    val myRef = database.getReference(DataBase.PATH_FIXED_BUMPS)
    private var childEventListener: ChildEventListener? = null
    val fixedBumpList = ArrayList<String>()
    val fixedBumpObjects = ArrayList<FixedBump>()
    val fixedBumpKeys = ArrayList<String>()
    val TAG = "FB REQUESTS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.reqList.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("latitude", fixedBumpObjects[position].latitude.toString())
            intent.putExtra("longitude", fixedBumpObjects[position].longitude.toString())
            intent.putExtra("size", fixedBumpObjects[position].size)
            intent.putExtra("keyBump", fixedBumpObjects[position].keyBump)
            intent.putExtra("keyReport", fixedBumpKeys[position])
            startActivity(intent)
        }


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

    override fun onPause() {
        super.onPause()
        if (childEventListener != null) {
            myRef.removeEventListener(childEventListener!!)
        }
    }

    override fun onResume() {
        super.onResume()

        //lista de reparados
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fixedBumpList)
        binding.reqList.adapter = adapter

        childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newFixedBump = dataSnapshot.getValue(FixedBump::class.java)
                if(newFixedBump != null){
                    val report = newFixedBump.latitude.toString() + ";" + newFixedBump.longitude
                    adapter.add(report)
                    fixedBumpObjects.add(newFixedBump)
                    dataSnapshot.key?.let { fixedBumpKeys.add(it) }
                }

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // A FixedBump has changed
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val removedFixedBump = dataSnapshot.getValue(FixedBump::class.java)
                if(removedFixedBump != null){
                    val report = removedFixedBump.latitude.toString() + ";" + removedFixedBump.longitude
                    adapter.remove(report)
                    fixedBumpObjects.remove(removedFixedBump)
                    dataSnapshot.key?.let { fixedBumpKeys.remove(it) }
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // A FixedBump has changed position
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException())
            }
        }

        childEventListener?.let { myRef.addChildEventListener(it) }

    }

}