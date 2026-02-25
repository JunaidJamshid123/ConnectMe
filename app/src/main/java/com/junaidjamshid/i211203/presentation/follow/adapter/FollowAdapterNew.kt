package com.junaidjamshid.i211203.presentation.follow.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.follow.FollowUser
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for followers/following list.
 */
class FollowAdapterNew(
    private val onUserClick: (FollowUser) -> Unit,
    private val onFollowClick: (FollowUser) -> Unit,
    private val onRemoveClick: ((FollowUser) -> Unit)?,
    private val showRemoveButton: Boolean = false
) : ListAdapter<FollowUser, FollowAdapterNew.FollowViewHolder>(FollowUserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_item, parent, false)
        return FollowViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class FollowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.iv_user_image)
        private val usernameText: TextView = itemView.findViewById(R.id.tv_username)
        private val fullNameText: TextView = itemView.findViewById(R.id.tv_full_name)
        private val removeButton: ImageView = itemView.findViewById(R.id.iv_remove)
        
        fun bind(followUser: FollowUser) {
            val user = followUser.user
            
            usernameText.text = user.username
            fullNameText.text = user.fullName
            
            // Load profile image
            if (!user.profilePicture.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(user.profilePicture, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile)
            }
            
            // Follow button - removed since not in layout
            // Follow/remove handled by click listeners
            
            // Remove button (for followers only)
            if (showRemoveButton && onRemoveClick != null) {
                removeButton.visibility = View.VISIBLE
                removeButton.setOnClickListener { onRemoveClick.invoke(followUser) }
            } else {
                removeButton.visibility = View.GONE
            }
            
            itemView.setOnClickListener { onUserClick(followUser) }
        }
    }
    
    class FollowUserDiffCallback : DiffUtil.ItemCallback<FollowUser>() {
        override fun areItemsTheSame(oldItem: FollowUser, newItem: FollowUser): Boolean {
            return oldItem.user.userId == newItem.user.userId
        }
        
        override fun areContentsTheSame(oldItem: FollowUser, newItem: FollowUser): Boolean {
            return oldItem == newItem
        }
    }
}
