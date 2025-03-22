package com.junaidjamshid.i211203.Adapters

import android.content.Context
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

        // Set likes count
        holder.likesCount.text = "0 likes"

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
        val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    holder.usernameText.text = username
                    holder.authorName.text = username

                    // Load profile image if exists
                    val profileImageBase64 = snapshot.child("profileImageBase64").getValue(String::class.java)
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
                    } else {
                        holder.heartIcon.setImageResource(R.drawable.simple_heart)
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
                    } else {
                        holder.saveIcon.setImageResource(R.drawable.save_instagram)
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

            if (isCurrentlyLiked) {
                holder.heartIcon.setImageResource(R.drawable.simple_heart)
                holder.heartIcon.tag = false
            } else {
                holder.heartIcon.setImageResource(R.drawable.heart)
                holder.heartIcon.tag = true
            }

            listener?.onLikeClicked(post.postId, !isCurrentlyLiked)
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

            if (isCurrentlySaved) {
                holder.saveIcon.setImageResource(R.drawable.save_instagram)
                holder.saveIcon.tag = false
            } else {
                holder.saveIcon.setImageResource(R.drawable.save_instagram)
                holder.saveIcon.tag = true
            }

            listener?.onSaveClicked(post.postId, !isCurrentlySaved)
        }

        // Profile click
        holder.profileImage.setOnClickListener {
            listener?.onProfileClicked(post.userId)
        }
        holder.usernameText.setOnClickListener {
            listener?.onProfileClicked(post.userId)
        }

        // Menu dots click
        holder.menuDots.setOnClickListener {
            listener?.onMenuClicked(post, position)
        }
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