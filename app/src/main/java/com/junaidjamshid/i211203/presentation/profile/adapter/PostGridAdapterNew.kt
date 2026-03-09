package com.junaidjamshid.i211203.presentation.profile.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post

/**
 * Clean Architecture Post Grid Adapter for profile.
 */
class PostGridAdapterNew(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, PostGridAdapterNew.PostViewHolder>(PostDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)

        fun bind(post: Post) {
            // Use allImages (handles both imageUrls and postImageUrl fallback)
            val firstImage = post.allImages.firstOrNull() ?: post.postImageUrl
            if (firstImage.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(firstImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        postImage.setImageBitmap(bitmap)
                    } else {
                        postImage.setBackgroundColor(0xFFEFEFEF.toInt())
                    }
                } catch (e: Exception) {
                    postImage.setBackgroundColor(0xFFEFEFEF.toInt())
                }
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }
    
    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }
        
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
