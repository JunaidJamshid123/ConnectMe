package com.junaidjamshid.i211203.Adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.models.Post
import com.junaidjamshid.i211203.PostDetailActivity

class PostGridAdapter(private val context: Context, private val posts: List<Post>) :
    RecyclerView.Adapter<PostGridAdapter.PostViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.post_image)

        init {
            // Set click listener to open post details
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = posts[position]
                    val intent = Intent(context, PostDetailActivity::class.java)
                    intent.putExtra("postId", post.postId)
                    context.startActivity(intent)
                }
            }

            // Set long click listener to delete post
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = posts[position]
                    showDeleteConfirmationDialog(post)
                    return@setOnLongClickListener true
                }
                return@setOnLongClickListener false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Decode and display image from byte array or Base64 string
        try {
            // Check if the image is stored as a Base64 string
            if (post.postImageUrl.isNotEmpty()) {
                // Decode Base64 string to byte array
                val imageBytes = Base64.decode(post.postImageUrl, Base64.DEFAULT)
                // Convert byte array to bitmap
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } else {
                // If no image, show a placeholder
                holder.postImage.setImageResource(R.drawable.avatar)
            }
        } catch (e: Exception) {
            // Handle any decoding errors
            Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            holder.postImage.setImageResource(R.drawable.avatar)
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        // First check if this post belongs to the current user
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.uid != post.userId) {
            Toast.makeText(context, "You can only delete your own posts", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { dialog, _ ->
                deletePost(post.postId)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deletePost(postId: String) {
        // Reference to the post in Firebase
        val postRef = database.child("posts").child(postId)

        // Delete the post
        postRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}