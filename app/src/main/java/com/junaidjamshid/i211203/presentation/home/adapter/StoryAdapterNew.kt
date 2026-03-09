package com.junaidjamshid.i211203.presentation.home.adapter

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
import com.junaidjamshid.i211203.domain.model.Story
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Instagram-style Story Adapter for home feed.
 * Groups stories by user and shows gradient ring for unviewed, grey ring for viewed.
 */
class StoryAdapterNew(
    private val onStoryClick: (Story) -> Unit
) : ListAdapter<Story, StoryAdapterNew.StoryViewHolder>(StoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stroies, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val storyImage: CircleImageView = itemView.findViewById(R.id.imgStoryProfile)
        private val usernameText: TextView = itemView.findViewById(R.id.tvUsername)
        private val gradientRing: View = itemView.findViewById(R.id.gradient_ring)

        fun bind(story: Story) {
            usernameText.text = story.username
            
            // Load profile image
            val imageToLoad = story.userProfileImage.ifEmpty { story.storyImageUrl }
            if (imageToLoad.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(imageToLoad, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        storyImage.setImageBitmap(bitmap)
                    } else {
                        storyImage.setImageResource(R.drawable.default_profile)
                    }
                } catch (e: Exception) {
                    storyImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                storyImage.setImageResource(R.drawable.default_profile)
            }
            
            // Ring color: gradient for unviewed, grey for viewed
            if (story.isViewedByCurrentUser) {
                gradientRing.setBackgroundResource(R.drawable.story_ring_viewed)
            } else {
                gradientRing.setBackgroundResource(R.drawable.story_gradient)
            }
            gradientRing.visibility = View.VISIBLE
            
            itemView.setOnClickListener { onStoryClick(story) }
        }
    }

    class StoryDiffCallback : DiffUtil.ItemCallback<Story>() {
        override fun areItemsTheSame(oldItem: Story, newItem: Story) =
            oldItem.storyId == newItem.storyId

        override fun areContentsTheSame(oldItem: Story, newItem: Story) =
            oldItem == newItem
    }
}
