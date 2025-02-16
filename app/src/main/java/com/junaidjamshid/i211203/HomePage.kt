package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class HomePage : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_home_page)

        val home = findViewById<LinearLayout>(R.id.Home)
        val search = findViewById<LinearLayout>(R.id.Search)
        val newPost = findViewById<LinearLayout>(R.id.NewPost)
        val profile = findViewById<LinearLayout>(R.id.Profile)
        val contacts = findViewById<LinearLayout>(R.id.Contacts)
        val dMs = findViewById<ImageView>(R.id.DMs);
        home.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        dMs.setOnClickListener{
            val intent = Intent(this, DMs::class.java)
             startActivity(intent)
        }
        search.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        newPost.setOnClickListener {
            val intent = Intent(this, newPostNext::class.java)
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
