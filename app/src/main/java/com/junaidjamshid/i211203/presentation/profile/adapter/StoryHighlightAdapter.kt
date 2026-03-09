package com.junaidjamshid.i211203.presentation.profile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R

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

        fun bind() {
            image.setImageResource(R.drawable.ic_add_circle_outline)
            image.setPadding(16, 16, 16, 16)
            name.text = "New"
            itemView.setOnClickListener { onAddHighlightClick() }
        }
    }

    inner class HighlightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.highlight_image)
        private val name: TextView = itemView.findViewById(R.id.highlight_name)

        fun bind(highlight: StoryHighlight) {
            name.text = highlight.name
            if (highlight.coverImageResId != 0) {
                image.setImageResource(highlight.coverImageResId)
            } else {
                image.setImageResource(R.drawable.default_profile)
            }
            image.setPadding(0, 0, 0, 0)
            itemView.setOnClickListener { onHighlightClick(highlight) }
        }
    }
}

/**
 * Data class representing a story highlight.
 */
data class StoryHighlight(
    val id: String = "",
    val name: String = "",
    val coverImageResId: Int = 0,
    val coverImageUrl: String = ""
)
