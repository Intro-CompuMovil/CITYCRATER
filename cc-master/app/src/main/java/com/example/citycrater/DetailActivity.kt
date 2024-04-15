package com.example.citycrater

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.citycrater.databinding.ActivityDetailBinding
import com.example.citycrater.databinding.ActivityFriendsBinding

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = this.intent // Get the Intent that launched this activity
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        val t: String = latitude.toString() + " ; " + longitude.toString()
        binding.txtlocation.text = t

    }
}