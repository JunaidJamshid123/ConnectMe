package com.junaidjamshid.i211203.Adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.models.Story
import java.util.*

class StoryAdapter(private val context: Context) :
    RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val TAG = "StoryAdapter"
    private var stories = mutableListOf<Story>()
    private var storyListener: OnStoryClickListener? = null

    // Interface for click events
    interface OnStoryClickListener {
        fun onStoryClick(story: Story, position: Int)
    }

    // Set listener
    fun setOnStoryClickListener(listener: OnStoryClickListener) {
        this.storyListener = listener
    }

    // ViewHolder class
    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardViewStory: CardView = itemView.findViewById(R.id.cardViewStory)
        val imgStoryProfile: ShapeableImageView = itemView.findViewById(R.id.imgStoryProfile)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)

        init {
            // Set click listener for the whole item
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    storyListener?.onStoryClick(stories[position], position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.stroies, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        // Set username
        holder.tvUsername.text = story.username

        // Set profile image from Base64 string
        if (story.userProfileImage.isNotEmpty()) {
            try {
                val bitmap = decodeBase64Image(story.userProfileImage)
                bitmap?.let {
                    holder.imgStoryProfile.setImageBitmap(it)
                } ?: holder.imgStoryProfile.setImageResource(R.drawable.junaid1)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile image: ${e.message}")
                holder.imgStoryProfile.setImageResource(R.drawable.junaid1)
            }
        } else {
            holder.imgStoryProfile.setImageResource(R.drawable.junaid1)
        }

        // Check if story is viewed
        val currentTime = System.currentTimeMillis()

        // Apply different style based on whether the story has expired
        if (currentTime > story.expiryTimestamp) {
            // Story expired - make it gray or change appearance
            holder.imgStoryProfile.alpha = 0.7f
            // Optionally change border
        }
    }

    override fun getItemCount(): Int = stories.size

    // Update the stories list
    fun setStories(newStories: List<Story>) {
        // Filter out expired stories
        val currentTime = System.currentTimeMillis()
        val validStories = newStories.filter { it.expiryTimestamp > currentTime }

        // Sort by timestamp, newest first
        val sortedStories = validStories.sortedByDescending { it.timestamp }

        stories.clear()
        stories.addAll(sortedStories)
        notifyDataSetChanged()
    }

    // Helper function to decode Base64 string to Bitmap
    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64 image: ${e.message}")
            null
        }
    }
}