package com.junaidjamshid.i211203.presentation.search.adapter

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
import com.junaidjamshid.i211203.domain.model.User
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for recent searches.
 */
class RecentSearchAdapterNew(
    private val onUserClick: (User) -> Unit,
    private val onRemoveClick: (User) -> Unit
) : ListAdapter<User, RecentSearchAdapterNew.RecentSearchViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_item, parent, false)
        return RecentSearchViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RecentSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.iv_user_image)
        private val usernameText: TextView = itemView.findViewById(R.id.tv_username)
        private val fullNameText: TextView = itemView.findViewById(R.id.tv_full_name)
        private val removeButton: ImageView = itemView.findViewById(R.id.iv_remove)
        
        fun bind(user: User) {
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
            
            // Show remove button for recent searches
            removeButton.visibility = View.VISIBLE
            removeButton.setOnClickListener { onRemoveClick(user) }
            
            itemView.setOnClickListener { onUserClick(user) }
        }
    }
    
    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }
        
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
