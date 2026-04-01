package com.junaidjamshid.i211203.presentation.highlight.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.highlight.SelectableStory

/**
 * Adapter for displaying selectable stories in the create highlight flow.
 * Enhanced with animations and selection order numbers.
 */
class SelectableStoryAdapter(
    private val onStoryClick: (SelectableStory) -> Unit
) : ListAdapter<SelectableStory, SelectableStoryAdapter.StoryViewHolder>(StoryDiffCallback()) {

    // Track selection order for numbering
    private val selectionOrder = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selectable_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateSelectionOrder(selectedIds: Set<String>) {
        // Remove deselected items from order
        selectionOrder.removeAll { it !in selectedIds }
        // Add newly selected items
        selectedIds.forEach { id ->
            if (id !in selectionOrder) {
                selectionOrder.add(id)
            }
        }
    }

    fun getSelectionNumber(storyId: String): Int {
        return selectionOrder.indexOf(storyId) + 1
    }

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgStory: ImageView = itemView.findViewById(R.id.imgStory)
        private val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)
        private val selectionBorder: View? = itemView.findViewById(R.id.selectionBorder)
        private val imgCheck: ImageView = itemView.findViewById(R.id.imgCheck)
        private val circleOutline: View = itemView.findViewById(R.id.circleOutline)
        private val tvSelectionNumber: TextView? = itemView.findViewById(R.id.tvSelectionNumber)

        fun bind(story: SelectableStory) {
            // Load image with smooth transition
            Glide.with(itemView.context)
                .load(story.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .placeholder(R.drawable.placeholder_story)
                .error(R.drawable.placeholder_story)
                .into(imgStory)

            // Show/hide selection state with animations
            if (story.isSelected) {
                selectionOverlay.visibility = View.VISIBLE
                selectionBorder?.visibility = View.VISIBLE
                circleOutline.visibility = View.GONE
                
                // Show selection number if available
                val selectionNum = getSelectionNumber(story.id)
                if (selectionNum > 0 && tvSelectionNumber != null) {
                    tvSelectionNumber.text = selectionNum.toString()
                    tvSelectionNumber.visibility = View.VISIBLE
                    imgCheck.visibility = View.GONE
                } else {
                    imgCheck.visibility = View.VISIBLE
                    tvSelectionNumber?.visibility = View.GONE
                }
            } else {
                selectionOverlay.visibility = View.GONE
                selectionBorder?.visibility = View.GONE
                imgCheck.visibility = View.GONE
                tvSelectionNumber?.visibility = View.GONE
                circleOutline.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                // Add scale animation on click
                val scaleDown = AnimationUtils.loadAnimation(itemView.context, android.R.anim.fade_in)
                scaleDown.duration = 100
                itemView.startAnimation(scaleDown)
                
                onStoryClick(story)
            }
        }
    }

    private class StoryDiffCallback : DiffUtil.ItemCallback<SelectableStory>() {
        override fun areItemsTheSame(oldItem: SelectableStory, newItem: SelectableStory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SelectableStory, newItem: SelectableStory): Boolean {
            return oldItem == newItem
        }
    }
}
