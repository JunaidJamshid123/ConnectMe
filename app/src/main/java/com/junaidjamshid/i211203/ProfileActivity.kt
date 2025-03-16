package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth

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
        val followers_btn = findViewById<LinearLayout>(R.id.followers)
        val following_btn = findViewById<LinearLayout>(R.id.following)
        val edit = findViewById<ImageView>(R.id.edit_profile)
        val logout = findViewById<ImageView>(R.id.logout)

        // Set up logout click listener
        logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        edit.setOnClickListener{
            val intent = Intent(this, EditProfile::class.java)
            startActivity(intent)
        }

        following_btn.setOnClickListener{
            val intent = Intent(this,Following::class.java)
            startActivity(intent)
        }

        followers_btn.setOnClickListener{
            val intent = Intent(this, Followers::class.java)
            startActivity(intent)
        }

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

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { _, _ ->
            // Perform logout operation
            performLogout()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            // Dismiss the dialog
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun performLogout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Navigate to login screen
        val intent = Intent(this, LoginScreem::class.java)
        // Clear the back stack so user can't go back after logout
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}