package com.junaidjamshid.i211203

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class chats : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_chats)
        val telephone = findViewById<ImageView>(R.id.telephone);
        val camera = findViewById<ImageView>(R.id.camera);

        telephone.setOnClickListener{
            val intent = Intent(this,Calls::class.java);
            startActivity(intent);
        }

        camera.setOnClickListener{
            val intent = Intent(this,VideoCalls::class.java);
            startActivity(intent);
        }
    }
}