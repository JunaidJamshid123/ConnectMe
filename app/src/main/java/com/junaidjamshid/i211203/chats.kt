package com.junaidjamshid.i211203

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class chats : AppCompatActivity() {
    // Firebase components
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var userProfileImage: CircleImageView
    private lateinit var txtUserName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_chats)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI components
        btnBack = findViewById(R.id.btnBack)
        userProfileImage = findViewById(R.id.userProfileImage)
        txtUserName = findViewById(R.id.txtUserName)

        // Retrieve user ID from intent
        val receiverUserId = intent.getStringExtra("USER_ID")
        if (receiverUserId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch and display user details
        fetchUserDetails(receiverUserId)

        // Setup back button
        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun fetchUserDetails(userId: String) {
        val userRef = database.reference.child("Users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Fetch user name
                    val userName = snapshot.child("username").getValue(String::class.java) ?: "Unknown User"
                    txtUserName.text = userName

                    // Fetch and display profile image
                    val profileImageData = snapshot.child("profilePicture").getValue(String::class.java)

                    if (!profileImageData.isNullOrEmpty()) {
                        try {
                            // Decode Base64 string to bitmap
                            val imageBytes = Base64.decode(profileImageData, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            userProfileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Set default profile image if decoding fails
                            userProfileImage.setImageResource(R.drawable.default_profile)
                        }
                    } else {
                        // Set default profile image if no image data
                        userProfileImage.setImageResource(R.drawable.default_profile)
                    }
                } else {
                    Toast.makeText(this@chats, "User not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chats, "Failed to fetch user details: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}