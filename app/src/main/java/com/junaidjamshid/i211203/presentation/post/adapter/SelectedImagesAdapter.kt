package com.junaidjamshid.i211203.presentation.post.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R

/**
 * Adapter for selected images thumbnail strip in the create post screen.
 * Shows small thumbnails of all selected images with remove buttons.
 */
class SelectedImagesAdapter(
    private val onRemoveClick: (Int) -> Unit,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ThumbViewHolder>() {

    private val images = mutableListOf<Bitmap>()
    private var selectedPosition = 0

    fun submitList(bitmaps: List<Bitmap>) {
        images.clear()
        images.addAll(bitmaps)
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_thumb, parent, false)
        return ThumbViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount() = images.size

    inner class ThumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbImage: ImageView = itemView.findViewById(R.id.iv_thumb)
        private val removeBtn: ImageView = itemView.findViewById(R.id.iv_remove)
        private val selectionBorder: View = itemView.findViewById(R.id.selection_border)

        fun bind(bitmap: Bitmap, position: Int) {
            thumbImage.setImageBitmap(bitmap)
            selectionBorder.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

            removeBtn.setOnClickListener { onRemoveClick(position) }
            itemView.setOnClickListener { onItemClick(position) }
        }
    }
}
