package com.junaidjamshid.i211203

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout

class DMs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_dms)

        val edit_icon = findViewById<ImageView>(R.id.edit);
        edit_icon.setOnClickListener{
            val intent = Intent(this, EditProfile::class.java)
            startActivity(intent)
        }

        val dms = findViewById<RelativeLayout>(R.id.chat1);
        dms.setOnClickListener{
            val intent = Intent(this, chats::class.java)
            startActivity(intent)
        }
    }
}