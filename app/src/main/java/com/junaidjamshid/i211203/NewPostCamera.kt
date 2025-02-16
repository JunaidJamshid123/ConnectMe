package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony.Mms.Intents
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout

class NewPostCamera : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide();
        setContentView(R.layout.activity_new_post_camera)
        val share = findViewById<Button>(R.id.share);
        share.setOnClickListener{
            val intent = Intent(this,HomePage::class.java);
            startActivity(intent);

        }

    }
}