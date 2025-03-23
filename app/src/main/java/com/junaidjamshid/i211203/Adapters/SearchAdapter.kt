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

class SearchAdapter(
    private val context: Context,
    private var userList: ArrayList<User>,
    private val listener: OnUserClickListener
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    interface OnUserClickListener {
        fun onUserClick(user: User, position: Int)
        fun onRemoveClick(user: User, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.search_item, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val user = userList[position]

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
                // Set default image if there's an error
                holder.ivUserImage.setImageResource(R.drawable.junaid1)
            }
        } ?: run {
            // Set default image if no profile picture
            holder.ivUserImage.setImageResource(R.drawable.junaid1)
        }

        // Set click listeners
        holder.itemView.setOnClickListener {
            listener.onUserClick(user, position)
        }

        holder.ivRemove.setOnClickListener {
            listener.onRemoveClick(user, position)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    fun updateList(newList: ArrayList<User>) {
        userList = newList
        notifyDataSetChanged()
    }

    fun filterUsers(query: String): ArrayList<User> {
        val filteredList = ArrayList<User>()

        if (query.isEmpty()) {
            return filteredList
        }

        val lowerCaseQuery = query.lowercase()
        for (user in userList) {
            if (user.username.lowercase().contains(lowerCaseQuery) ||
                user.fullName.lowercase().contains(lowerCaseQuery)) {
                filteredList.add(user)
            }
        }

        return filteredList
    }

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivUserImage: ImageView = itemView.findViewById(R.id.iv_user_image)
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvFullName: TextView = itemView.findViewById(R.id.tv_full_name)
        val tvStats: TextView = itemView.findViewById(R.id.tv_stats)
        val ivRemove: ImageView = itemView.findViewById(R.id.iv_remove)
    }
}