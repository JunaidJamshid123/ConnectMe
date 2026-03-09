package com.junaidjamshid.i211203.presentation.follow.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.follow.FollowUser
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Instagram-style adapter for followers/following list.
 */
class FollowAdapterNew(
    private val onUserClick: (FollowUser) -> Unit,
    private val onFollowClick: (FollowUser) -> Unit,
    private val onRemoveClick: ((FollowUser) -> Unit)?,
    private val showRemoveButton: Boolean = false,
    private val isCurrentUserProfile: Boolean = true
) : ListAdapter<FollowUser, FollowAdapterNew.FollowViewHolder>(FollowUserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follower, parent, false)
        return FollowViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class FollowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.username)
        private val fullNameText: TextView = itemView.findViewById(R.id.full_name)
        private val actionButton: MaterialButton = itemView.findViewById(R.id.btn_action)
        private val removeButton: ImageView = itemView.findViewById(R.id.btn_remove)
        
        private fun dpToPx(dp: Float): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, itemView.context.resources.displayMetrics
            ).toInt()
        }
        
        fun bind(followUser: FollowUser) {
            val user = followUser.user
            val context = itemView.context
            
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
            
            // Configure action button based on follow state
            when {
                followUser.isFollowing -> {
                    // Already following - show "Following" button (gray background like Instagram)
                    actionButton.text = context.getString(R.string.following)
                    actionButton.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    actionButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFEFEFEF.toInt())
                    actionButton.strokeColor = android.content.res.ColorStateList.valueOf(0xFFDBDBDB.toInt())
                    actionButton.strokeWidth = dpToPx(1f)
                }
                followUser.isFollowedBy && !followUser.isFollowing -> {
                    // They follow you but you don't follow them - show "Follow Back" (blue)
                    actionButton.text = context.getString(R.string.follow_back)
                    actionButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    actionButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF0095F6.toInt())
                    actionButton.strokeColor = android.content.res.ColorStateList.valueOf(0xFF0095F6.toInt())
                    actionButton.strokeWidth = 0
                }
                else -> {
                    // Not following - show "Follow" button (blue)
                    actionButton.text = context.getString(R.string.follow)
                    actionButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    actionButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF0095F6.toInt())
                    actionButton.strokeColor = android.content.res.ColorStateList.valueOf(0xFF0095F6.toInt())
                    actionButton.strokeWidth = 0
                }
            }
            
            actionButton.setOnClickListener { onFollowClick(followUser) }
            
            // Show remove button only for own profile's followers list
            if (showRemoveButton && isCurrentUserProfile && onRemoveClick != null) {
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
