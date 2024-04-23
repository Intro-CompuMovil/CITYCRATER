package com.example.citycrater.users

import android.content.Intent
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat.startActivity
import com.example.citycrater.RequestsActivity

class UserSessionManager {
    companion object{
        const val DRIVER = "Driver"
        const val ADMIN = "Admin"
        lateinit var CURRENT: String
        const val PATH_USERS="users/"

        fun setUserView (button: ImageButton): Boolean{
            if(UserSessionManager.CURRENT == UserSessionManager.DRIVER){
                button.isEnabled = false
                button.setVisibility(View.GONE)
                return false
            }else if (UserSessionManager.CURRENT == UserSessionManager.ADMIN){
                button.isEnabled = true
                button.setVisibility(View.VISIBLE)
                return true
            }
            return false
        }
    }
}