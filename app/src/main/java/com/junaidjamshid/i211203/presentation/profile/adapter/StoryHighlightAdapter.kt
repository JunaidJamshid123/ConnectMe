package com.junaidjamshid.i211203.presentation.profile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.StoryHighlight

/**
 * Adapter for Instagram-style story highlights on the profile screen.
 */
class StoryHighlightAdapter(
    private val onHighlightClick: (StoryHighlight) -> Unit = {},
    private val onAddHighlightClick: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_HIGHLIGHT = 1
    }

    private val highlights = mutableListOf<StoryHighlight>()

    fun submitList(list: List<StoryHighlight>) {
        highlights.clear()
        highlights.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_ADD else TYPE_HIGHLIGHT
    }

    override fun getItemCount(): Int = highlights.size + 1 // +1 for add button

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_highlight, parent, false)
        return if (viewType == TYPE_ADD) AddViewHolder(view) else HighlightViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AddViewHolder -> holder.bind()
            is HighlightViewHolder -> holder.bind(highlights[position - 1])
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.highlight_image)
        private val name: TextView = itemView.findViewById(R.id.highlight_name)
        private val highlightRing: View? = itemView.findViewById(R.id.highlightRing)

        fun bind() {
            // Show simple border for add button, not gradient
            highlightRing?.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_highlight_add_border)
            
            image.setImageResource(R.drawable.ic_add)
            image.setPadding(24, 24, 24, 24)
            image.setColorFilter(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            name.text = "New"
            name.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            itemView.setOnClickListener { onAddHighlightClick() }
        }
    }

    inner class HighlightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.highlight_image)
        private val name: TextView = itemView.findViewById(R.id.highlight_name)
        private val highlightRing: View? = itemView.findViewById(R.id.highlightRing)

        fun bind(highlight: StoryHighlight) {
            name.text = highlight.name
            name.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            
            // Show gradient ring for highlights
            highlightRing?.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_highlight_ring_gradient)
            
            // Load cover image from URL using Glide with smooth transition
            image.setPadding(0, 0, 0, 0)
            image.clearColorFilter()
            
            if (highlight.coverImageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(highlight.coverImageUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(image)
            } else if (highlight.stories.isNotEmpty()) {
                // Use first story image as cover if no cover set
                Glide.with(itemView.context)
                    .load(highlight.stories.first().storyImageUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(image)
            } else {
                image.setImageResource(R.drawable.default_profile)
            }
            
            itemView.setOnClickListener { onHighlightClick(highlight) }
        }
    }
}
