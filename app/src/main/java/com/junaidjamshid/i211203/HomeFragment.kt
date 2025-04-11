package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.junaidjamshid.i211203.Adapters.PostAdapter
import com.junaidjamshid.i211203.Adapters.StoryAdapter
import com.junaidjamshid.i211203.models.Post
import com.junaidjamshid.i211203.models.Story
import java.util.*

class HomeFragment : Fragment(), PostAdapter.OnPostInteractionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val TAG = "HomeFragment"
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val addStroy = view.findViewById<FrameLayout>(R.id.addStroy)
        val dms = view.findViewById<ImageView>(R.id.DMs)
        val currentUserImage = view.findViewById<ImageView>(R.id.current_user_image)

        dms.setOnClickListener{
            val intent = Intent(context, DMs::class.java)
            startActivity(intent)
        }

        addStroy.setOnClickListener{
            val intent = Intent(context, newPostNext::class.java)
            startActivity(intent)
        }

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("posts")

        // Load current user's profile image
        loadCurrentUserProfileImage(currentUserImage)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter
        postAdapter = PostAdapter(requireContext())
        postAdapter.setOnPostInteractionListener(this)
        recyclerView.adapter = postAdapter



        storiesRecyclerView = view.findViewById(R.id.recycler_view_stories)
        storiesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        storyAdapter = StoryAdapter(requireContext())

        storyAdapter.setOnStoryClickListener(object : StoryAdapter.OnStoryClickListener {
            override fun onStoryClick(story: Story, position: Int) {
                // This is optional since we're already handling the click in the adapter
                // But you could do additional processing here if needed
            }
        })
        storiesRecyclerView.adapter = storyAdapter
        // Load posts
        loadPosts()
        loadStories()
        return view
    }



    // Add this new function to load stories
    // Add this to your HomeFragment class


    // Replace your loadStories() function with this updated version if you want
    private fun loadStories() {
        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")

        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val storiesList = mutableListOf<Story>()

                // Group stories by user for better organization
                val userStoriesMap = mutableMapOf<String, MutableList<Story>>()

                for (storySnapshot in snapshot.children) {
                    try {
                        val story = storySnapshot.getValue(Story::class.java)
                        story?.let {
                            // Only add stories that haven't expired
                            val currentTime = System.currentTimeMillis()
                            if (it.expiryTimestamp > currentTime) {
                                // Group by userId
                                if (!userStoriesMap.containsKey(it.userId)) {
                                    userStoriesMap[it.userId] = mutableListOf()
                                }
                                userStoriesMap[it.userId]?.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing story: ${e.message}")
                    }
                }

                // Get the most recent story for each user
                for (userStories in userStoriesMap.values) {
                    // Sort by timestamp (newest first)
                    userStories.sortByDescending { it.timestamp }
                    // Add the most recent story to our list
                    storiesList.add(userStories.first())
                }

                // Update adapter with stories
                storyAdapter.setStories(storiesList)

                // Show empty state or handle no stories case if needed
                if (storiesList.isEmpty()) {
                    // Handle empty state
                    Log.d(TAG, "No active stories found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load stories: ${error.message}")
            }
        })
    }




    private fun loadCurrentUserProfileImage(imageView: ImageView) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Reference to user data in the database
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get profile picture as Base64 string
                    val profileImageBase64 = snapshot.child("profilePicture").getValue(String::class.java)

                    if (!profileImageBase64.isNullOrEmpty()) {
                        // Decode Base64 to bitmap and set to ImageView
                        val bitmap = decodeBase64Image(profileImageBase64)
                        bitmap?.let {
                            imageView.setImageBitmap(it)
                        } ?: run {
                            // Use default image if decoding fails
                            imageView.setImageResource(R.drawable.junaid1)
                        }
                    } else {
                        // Use default image if no profile picture
                        imageView.setImageResource(R.drawable.junaid1)
                    }
                } else {
                    // Use default image if user data doesn't exist
                    imageView.setImageResource(R.drawable.junaid1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load user profile image: ${error.message}")
                // Set default image on error
                imageView.setImageResource(R.drawable.junaid1)
            }
        })
    }

    // Helper function to decode Base64 string to Bitmap
    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64 image: ${e.message}")
            null
        }
    }

    private fun loadPosts() {
        // Show loading indicator if you have one
        // loadingIndicator.visibility = View.VISIBLE

        // Query to get posts ordered by timestamp (newest first)
        databaseRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<Post>()

                for (postSnapshot in snapshot.children) {
                    try {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.let {
                            // Add post to the beginning of the list (newest first)
                            posts.add(0, it)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing post: ${e.message}")
                    }
                }

                // Update adapter with new posts
                postAdapter.setPosts(posts)

                // Hide loading indicator if you have one
                // loadingIndicator.visibility = View.GONE

                // Show empty state if no posts
                if (posts.isEmpty()) {
                    // emptyStateView.visibility = View.VISIBLE
                    Toast.makeText(context, "No posts found", Toast.LENGTH_SHORT).show()
                } else {
                    // emptyStateView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(context, "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()

                // Hide loading indicator if you have one
                // loadingIndicator.visibility = View.GONE
            }
        })
    }

    // Implementation of PostAdapter.OnPostInteractionListener

    override fun onLikeClicked(postId: String, isLiked: Boolean) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val likeRef = FirebaseDatabase.getInstance().getReference("likes")
            .child(postId).child(userId)

        if (isLiked) {
            // Add like
            likeRef.setValue(true).addOnSuccessListener {
                updateLikesCount(postId)
            }
        } else {
            // Remove like
            likeRef.removeValue().addOnSuccessListener {
                updateLikesCount(postId)
            }
        }
    }

    private fun updateLikesCount(postId: String) {
        // Count likes for this post
        val likesRef = FirebaseDatabase.getInstance().getReference("likes").child(postId)
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likesCount = snapshot.childrenCount

                // Update post's likes count in the database
                val postRef = FirebaseDatabase.getInstance().getReference("posts")
                    .child(postId).child("likesCount")
                postRef.setValue(likesCount)

                // You might also want to update the UI directly here
                // This is handled by the adapter through the data change listener
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error updating likes count: ${error.message}")
            }
        })
    }

    override fun onCommentClicked(postId: String) {
        // Navigate to comments fragment or open comments dialog
        // For example:
        // val commentsFragment = CommentsFragment.newInstance(postId)
        // requireActivity().supportFragmentManager.beginTransaction()
        //     .replace(R.id.fragment_container, commentsFragment)
        //     .addToBackStack(null)
        //     .commit()

        Toast.makeText(context, "Comments for post $postId", Toast.LENGTH_SHORT).show()
    }

    override fun onShareClicked(postId: String) {
        // Implement share functionality
        // For example, share the post content using Intent
        Toast.makeText(context, "Share post $postId", Toast.LENGTH_SHORT).show()
    }

    override fun onSaveClicked(postId: String, isSaved: Boolean) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val saveRef = FirebaseDatabase.getInstance().getReference("saves")
            .child(userId).child(postId)

        if (isSaved) {
            // Save post
            saveRef.setValue(true).addOnSuccessListener {
                Toast.makeText(context, "Post saved", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Remove from saved
            saveRef.removeValue().addOnSuccessListener {
                Toast.makeText(context, "Post removed from saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onProfileClicked(userId: String) {
        // Navigate to user profile
        // For example:
        // val profileFragment = ProfileFragment.newInstance(userId)
        // requireActivity().supportFragmentManager.beginTransaction()
        //     .replace(R.id.fragment_container, profileFragment)
        //     .addToBackStack(null)
        //     .commit()

        Toast.makeText(context, "Navigate to profile of user $userId", Toast.LENGTH_SHORT).show()
    }

    override fun onMenuClicked(post: Post, position: Int) {
        // Show popup menu with options like delete, report, etc.
        // Check if current user is the post owner to show delete option

        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid == post.userId) {
            // Show owner options (delete, edit, etc.)
            // For example:
            // val popupMenu = PopupMenu(context, view)
            // popupMenu.menuInflater.inflate(R.menu.post_owner_menu, popupMenu.menu)
            // popupMenu.setOnMenuItemClickListener { menuItem ->
            //     when (menuItem.itemId) {
            //         R.id.menu_delete -> deletePost(post.postId)
            //         R.id.menu_edit -> editPost(post)
            //     }
            //     true
            // }
            // popupMenu.show()

            Toast.makeText(context, "Show owner options for post ${post.postId}", Toast.LENGTH_SHORT).show()
        } else {
            // Show regular user options (report, etc.)
            Toast.makeText(context, "Show regular user options for post ${post.postId}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePost(postId: String) {
        // Confirm deletion with dialog
        // Then delete from Firebase

        FirebaseDatabase.getInstance().getReference("posts")
            .child(postId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}