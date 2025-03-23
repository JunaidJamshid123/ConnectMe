package com.junaidjamshid.i211203.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.models.User

class RecentSearchAdapter(
    private val context: Context,
    private var recentSearches: ArrayList<User>,
    private val listener: OnRecentSearchListener
) : RecyclerView.Adapter<RecentSearchAdapter.RecentSearchViewHolder>() {

    interface OnRecentSearchListener {
        fun onRecentSearchClick(user: User, position: Int)
        fun onRemoveRecentSearch(user: User, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.search_item, parent, false)
        return RecentSearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        val user = recentSearches[position]

        // Set user data
        holder.tvUsername.text = user.username
        holder.tvFullName.text = user.fullName

        // Set stats (followers and following)
        val followersCount = user.followers.size
        val followingCount = user.following.size
        holder.tvStats.text = "$followersCount followers · $followingCount following"

        // Set profile image if available
        user.profilePicture?.let {
            try {
                val imageBytes = Base64.decode(it, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivUserImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivUserImage.setImageResource(R.drawable.junaid1)
            }
        } ?: run {
            holder.ivUserImage.setImageResource(R.drawable.junaid1)
        }

        // Set click listeners
        holder.itemView.setOnClickListener {
            listener.onRecentSearchClick(user, position)
        }

        holder.ivRemove.setOnClickListener {
            listener.onRemoveRecentSearch(user, position)
        }
    }

    override fun getItemCount(): Int {
        return recentSearches.size
    }

    fun updateList(newList: ArrayList<User>) {
        recentSearches = newList
        notifyDataSetChanged()
    }

    class RecentSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivUserImage: ImageView = itemView.findViewById(R.id.iv_user_image)
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvFullName: TextView = itemView.findViewById(R.id.tv_full_name)
        val tvStats: TextView = itemView.findViewById(R.id.tv_stats)
        val ivRemove: ImageView = itemView.findViewById(R.id.iv_remove)
    }
}