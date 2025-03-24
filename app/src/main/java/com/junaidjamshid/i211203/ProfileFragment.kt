package com.junaidjamshid.i211203

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import com.junaidjamshid.i211203.models.User

class ProfileFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI Components
    private lateinit var profileImage: CircleImageView
    private lateinit var usernameText: TextView
    private lateinit var bioText: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        profileImage = view.findViewById(R.id.profile_image)
        usernameText = view.findViewById(R.id.username_text)
        bioText = view.findViewById(R.id.bio_text)
        postsCount = view.findViewById(R.id.posts_count)
        followersCount = view.findViewById(R.id.followers_count)
        followingCount = view.findViewById(R.id.following_count)

        // Set up button click listeners
        val editProfileButton = view.findViewById<ImageView>(R.id.edit_profile)
        val logoutButton = view.findViewById<ImageView>(R.id.logout)

        editProfileButton.setOnClickListener {
            navigateToEditProfileActivity()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Reference to the current user's data in the database
            val userRef = database.child("Users").child(userId)



            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        try {
                            // Get user as User object
                            val user = snapshot.getValue(User::class.java)

                            if (user != null) {
                                // Update UI with user data
                                usernameText.text = user.username
                                bioText.text = user.bio ?: "No bio yet"

                                // Set followers and following counts
                                followersCount.text = user.followers.size.toString()
                                followingCount.text = user.following.size.toString()

                                // Get profile picture from the User object
                                val profilePictureBytes = user.getProfilePictureBytes()

                                // Convert ByteArray to Bitmap and set profile image
                                if (profilePictureBytes != null && profilePictureBytes.isNotEmpty()) {
                                    try {
                                        val bitmap = BitmapFactory.decodeByteArray(
                                            profilePictureBytes,
                                            0,
                                            profilePictureBytes.size
                                        )
                                        profileImage.setImageBitmap(bitmap)
                                    } catch (e: Exception) {
                                        // If there's an error decoding the image, use default
                                        profileImage.setImageResource(R.drawable.junaid1)
                                        Toast.makeText(context, "Error loading profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Set default image if no profile picture exists
                                    profileImage.setImageResource(R.drawable.junaid1)
                                }
                            } else {
                                // If user object is null, use default values
                                usernameText.text = "Username"
                                bioText.text = "No bio yet"
                                followersCount.text = "0"
                                followingCount.text = "0"
                                profileImage.setImageResource(R.drawable.junaid1)
                            }

                            // Get posts count (assuming you have a Posts node with userId as key)
                            // This would need to be adjusted based on your actual database structure
                            database.child("Posts").orderByChild("userId").equalTo(userId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(postsSnapshot: DataSnapshot) {
                                        val posts = postsSnapshot.childrenCount.toString()
                                        postsCount.text = posts
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        postsCount.text = "0"
                                    }
                                })

                        } catch (e: Exception) {
                            Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // User not logged in, redirect to login
            val intent = Intent(requireActivity(), LoginScreem::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    // Function to navigate to the EditProfileActivity
    private fun navigateToEditProfileActivity() {
        val intent = Intent(requireActivity(), EditProfile::class.java)
        startActivity(intent)
    }

    // Function to show logout confirmation dialog
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // Function to perform the actual logout using Firebase
    private fun performLogout() {
        try {
            // Sign out from Firebase
            auth.signOut()

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate to login screen
            val intent = Intent(requireActivity(), LoginScreem::class.java)
            // Clear the back stack so user can't navigate back after logout
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}