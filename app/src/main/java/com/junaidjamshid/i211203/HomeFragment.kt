package com.junaidjamshid.i211203

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.junaidjamshid.i211203.Adapters.PostAdapter
import com.junaidjamshid.i211203.models.Comment
import com.junaidjamshid.i211203.models.Post
import java.util.*

class HomeFragment : Fragment(), PostAdapter.OnPostInteractionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val TAG = "HomeFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("posts")

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter
        postAdapter = PostAdapter(requireContext())
        postAdapter.setOnPostInteractionListener(this)
        recyclerView.adapter = postAdapter

        // Load posts
        loadPosts()

        return view
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