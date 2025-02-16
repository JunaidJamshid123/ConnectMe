package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.GridView
import android.widget.LinearLayout

class ProfileActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)

        val gridView = findViewById<GridView>(R.id.grid_view)

        val imageList = listOf(
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1
        )

        val adapter = ImageAdapter(this, imageList)
        gridView.adapter = adapter

        val home = findViewById<LinearLayout>(R.id.Home)
        val search = findViewById<LinearLayout>(R.id.Search)
        val newPost = findViewById<LinearLayout>(R.id.NewPost)
        val profile = findViewById<LinearLayout>(R.id.Profile)
        val contacts = findViewById<LinearLayout>(R.id.Contacts)

        home.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        search.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        newPost.setOnClickListener {
            val intent = Intent(this, NewPost::class.java)
            startActivity(intent)
        }

        profile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        contacts.setOnClickListener {
            val intent = Intent(this, ContactsPage::class.java)
            startActivity(intent)
        }

    }
}