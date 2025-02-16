package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.GridView
import android.widget.TextView

class NewPost : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_post)
        supportActionBar?.hide()
        val gridView = findViewById<GridView>(R.id.grid_view)
        val next_text = findViewById<TextView>(R.id.next_text);

        next_text.setOnClickListener{
            val intent = Intent(this,NewPostCamera::class.java);
            startActivity(intent)
        }

        val imageList = listOf(
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1
        )

        val adapter = newPostImageAdapter(this, imageList)
        gridView.adapter = adapter
    }
}