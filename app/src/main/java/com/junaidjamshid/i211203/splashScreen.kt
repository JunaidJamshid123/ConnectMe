package com.junaidjamshid.i211203

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // User is already logged in, navigate to HomePage
                startActivity(Intent(this, HomePage::class.java))
            } else {
                // User not logged in, navigate to LoginScreen
                startActivity(Intent(this, LoginScreem::class.java))
            }
            finish()
        }, 3000) // 3-second delay
    }
}
