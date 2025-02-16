package com.junaidjamshid.i211203

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class newPostNext : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide();
        setContentView(R.layout.activity_new_post_next)
        val next_text = findViewById<TextView>(R.id.next_text)
        val thumbnail = findViewById<ImageView>(R.id.thumbnail)

        next_text.setOnClickListener{
            val intent = Intent(this,NewPost::class.java);
            startActivity(intent);
        }

        thumbnail.setOnClickListener{
            val intent = Intent(this,NewPost::class.java);
            startActivity(intent);
        }


    }
}