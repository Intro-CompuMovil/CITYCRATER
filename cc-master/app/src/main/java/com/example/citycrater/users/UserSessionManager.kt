package com.example.citycrater.users

import android.content.Intent
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat.startActivity
import com.example.citycrater.RequestsActivity
import com.example.citycrater.model.User

class UserSessionManager {
    companion object{
        const val DRIVER = "Driver"
        const val ADMIN = "Admin"
        lateinit var CURRENT: User
        lateinit var CURRENT_UID: String

        fun setAdminReportsView (button: ImageButton): Boolean{
            if(CURRENT.usertype == DRIVER){
                button.isEnabled = false
                button.setVisibility(View.GONE)
                return false
            }else if (CURRENT.usertype == ADMIN){
                button.isEnabled = true
                button.setVisibility(View.VISIBLE)
                return true
            }
            return false
        }

        fun validateEmail(email: String): Boolean{
            if (!email.contains("@") ||
                !email.contains(".") ||
                email.length < 5)
                return false
            return true
        }

        fun validatePassword(password: String): Boolean{
            if (password.length < 8)
                return false
            return true
        }
    }
}