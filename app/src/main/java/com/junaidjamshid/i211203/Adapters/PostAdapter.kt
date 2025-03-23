package com.junaidjamshid.i211203.Adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.UserProfile
import com.junaidjamshid.i211203.models.Post
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(private val context: Context) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var postList: MutableList<Post> = mutableListOf()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    // Listener for post interactions
    interface OnPostInteractionListener {
        fun onLikeClicked(postId: String, isLiked: Boolean)
        fun onCommentClicked(postId: String)
        fun onShareClicked(postId: String)
        fun onSaveClicked(postId: String, isSaved: Boolean)
        fun onProfileClicked(userId: String)
        fun onMenuClicked(post: Post, position: Int)
    }

    private var listener: OnPostInteractionListener? = null

    fun setOnPostInteractionListener(listener: OnPostInteractionListener) {
        this.listener = listener
    }

    fun setPosts(posts: List<Post>) {
        this.postList.clear()
        this.postList.addAll(posts)
        notifyDataSetChanged()
    }

    fun addPost(post: Post) {
        this.postList.add(0, post)
        notifyItemInserted(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        // Load user profile data
        loadUserData(holder, post.userId)

        // Set post caption
        holder.authorName.text = post.username
        holder.postCaption.text = post.caption

        // Load likes count from Firebase
        fetchLikesCount(post.postId, holder)

        // Set timestamp
        holder.timestamp.text = getTimeAgo(post.timestamp)

        // Load post image from base64
        if (!post.postImageUrl.isNullOrEmpty()) {
            val bitmap = decodeBase64Image(post.postImageUrl)
            bitmap?.let {
                holder.postImage.setImageBitmap(it)
            } ?: run {
                holder.postImage.setImageResource(R.drawable.junaid1)
            }
        } else {
            holder.postImage.setImageResource(R.drawable.junaid1)
        }

        // Set location if available
        holder.locationText.text = "Your Location"

        // Check if post is liked by current user
        checkIfLiked(post.postId, holder)

        // Check if post is saved by current user
        checkIfSaved(post.postId, holder)

        // Set click listeners
        setupClickListeners(holder, post, position)
    }

    override fun getItemCount(): Int = postList.size

    private fun loadUserData(holder: PostViewHolder, userId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    holder.usernameText.text = username
                    holder.authorName.text = username

                    // Load profile image if exists
                    val profileImageBase64 = snapshot.child("profilePicture").getValue(String::class.java)
                    if (!profileImageBase64.isNullOrEmpty()) {
                        val bitmap = decodeBase64Image(profileImageBase64)
                        bitmap?.let {
                            holder.profileImage.setImageBitmap(it)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun fetchLikesCount(postId: String, holder: PostViewHolder) {
        // Method 1: Fetching from the post object in 'posts' node
        val postRef = FirebaseDatabase.getInstance().getReference("posts").child(postId)
        postRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val likesCount = snapshot.child("likesCount").getValue(Int::class.java) ?: 0
                    updateLikesCount(holder, likesCount)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        // Method 2: If likes are stored as a collection of user IDs, count them
        val likesRef = FirebaseDatabase.getInstance().getReference("likes").child(postId)
        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likesCount = snapshot.childrenCount.toInt()
                updateLikesCount(holder, likesCount)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun updateLikesCount(holder: PostViewHolder, count: Int) {
        holder.likesCount.text = if (count == 1) "$count like" else "$count likes"
    }

    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - timestamp

        val seconds = timeDifference / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "JUST NOW"
            minutes < 60 -> "$minutes MINUTES AGO"
            hours < 24 -> "$hours HOURS AGO"
            days < 7 -> "$days DAYS AGO"
            else -> {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date(timestamp)).uppercase(Locale.getDefault())
            }
        }
    }

    private fun checkIfLiked(postId: String, holder: PostViewHolder) {
        currentUser?.uid?.let { userId ->
            val likeRef = FirebaseDatabase.getInstance().getReference("likes")
                .child(postId).child(userId)

            likeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        holder.heartIcon.setImageResource(R.drawable.heart)
                        holder.heartIcon.tag = true
                    } else {
                        holder.heartIcon.setImageResource(R.drawable.simple_heart)
                        holder.heartIcon.tag = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun checkIfSaved(postId: String, holder: PostViewHolder) {
        currentUser?.uid?.let { userId ->
            val saveRef = FirebaseDatabase.getInstance().getReference("saves")
                .child(userId).child(postId)

            saveRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        holder.saveIcon.setImageResource(R.drawable.save_instagram)
                        holder.saveIcon.tag = true
                    } else {
                        holder.saveIcon.setImageResource(R.drawable.save_instagram)
                        holder.saveIcon.tag = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun setupClickListeners(holder: PostViewHolder, post: Post, position: Int) {
        // Heart/Like button click
        holder.heartIcon.setOnClickListener {
            val isCurrentlyLiked = (holder.heartIcon.tag as? Boolean) ?: false

            if (currentUser != null) {
                val likesRef = FirebaseDatabase.getInstance().getReference("likes")
                    .child(post.postId).child(currentUser.uid)

                val postRef = FirebaseDatabase.getInstance().getReference("posts")
                    .child(post.postId).child("likesCount")

                if (isCurrentlyLiked) {
                    // Unlike the post
                    holder.heartIcon.setImageResource(R.drawable.simple_heart)
                    holder.heartIcon.tag = false

                    // Remove like from likes collection
                    likesRef.removeValue()

                    // Decrement likes count
                    postRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                            if (currentLikes > 0) {
                                postRef.setValue(currentLikes - 1)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                } else {
                    // Like the post
                    holder.heartIcon.setImageResource(R.drawable.heart)
                    holder.heartIcon.tag = true

                    // Add like to likes collection
                    likesRef.setValue(true)

                    // Increment likes count
                    postRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                            postRef.setValue(currentLikes + 1)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                }

                listener?.onLikeClicked(post.postId, !isCurrentlyLiked)
            }
        }

        // Comment button click
        holder.commentIcon.setOnClickListener {
            listener?.onCommentClicked(post.postId)
        }

        // Share button click
        holder.sendIcon.setOnClickListener {
            listener?.onShareClicked(post.postId)
        }

        // Save button click
        holder.saveIcon.setOnClickListener {
            val isCurrentlySaved = (holder.saveIcon.tag as? Boolean) ?: false

            if (currentUser != null) {
                val saveRef = FirebaseDatabase.getInstance().getReference("saves")
                    .child(currentUser.uid).child(post.postId)

                if (isCurrentlySaved) {
                    // Unsave the post
                    holder.saveIcon.setImageResource(R.drawable.save_instagram)
                    holder.saveIcon.tag = false

                    // Remove from saves
                    saveRef.removeValue()
                } else {
                    // Save the post
                    holder.saveIcon.setImageResource(R.drawable.save_instagram)
                    holder.saveIcon.tag = true

                    // Add to saves
                    saveRef.setValue(true)
                }

                listener?.onSaveClicked(post.postId, !isCurrentlySaved)
            }
        }

        // Profile click - Added navigation to ProfileActivity
        holder.profileImage.setOnClickListener {
            navigateToUserProfile(post.userId)
        }

        // Username click - Added navigation to ProfileActivity
        holder.usernameText.setOnClickListener {
            navigateToUserProfile(post.userId)
        }

        // Menu dots click
        holder.menuDots.setOnClickListener {
            listener?.onMenuClicked(post, position)
        }
    }

    // New method to handle navigation to user profile
    private fun navigateToUserProfile(userId: String) {
        // Notify listener (optional - can be used for tracking or other purposes)
        listener?.onProfileClicked(userId)

        // Create intent to navigate to ProfileActivity
        val intent = Intent(context, UserProfile::class.java).apply {
            putExtra("USER_ID", userId)
            // Add any additional data needed for the profile page
        }

        // Start the ProfileActivity
        context.startActivity(intent)
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val usernameText: TextView = itemView.findViewById(R.id.username_text)
        val locationText: TextView = itemView.findViewById(R.id.location_text)
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val heartIcon: ImageView = itemView.findViewById(R.id.heart)
        val commentIcon: ImageView = itemView.findViewById(R.id.comment)
        val sendIcon: ImageView = itemView.findViewById(R.id.send)
        val saveIcon: ImageView = itemView.findViewById(R.id.save)
        val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        val authorName: TextView = itemView.findViewById(R.id.author_name)
        val postCaption: TextView = itemView.findViewById(R.id.post_caption)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val menuDots: ImageView = itemView.findViewById(R.id.menu_dots)

        init {
            // Set initial tags for togglable elements
            heartIcon.tag = false
            saveIcon.tag = false
        }
    }
}