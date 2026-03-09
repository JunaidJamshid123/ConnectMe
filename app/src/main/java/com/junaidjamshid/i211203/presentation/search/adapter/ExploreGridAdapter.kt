package com.junaidjamshid.i211203.presentation.search.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post

/**
 * Adapter for Instagram-style explore grid with mixed tile sizes.
 * 
 * Instagram's explore grid pattern repeats every 10 items in two groups:
 * 
 * Group A (indices 0-4):
 *   [S][S][L]   <- L is large (2 cols wide, 2 rows tall) at position 2
 *   [S][S][ ]   <- L continues here
 * 
 * Group B (indices 5-9):
 *   [L][S][S]   <- L is large at position 5 (left side)
 *   [ ][S][S]   <- L continues here
 */
class ExploreGridAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, ExploreGridAdapter.ExploreViewHolder>(PostDiffCallback()) {

    companion object {
        const val TYPE_SMALL = 0
        const val TYPE_LARGE = 1
    }

    /**
     * Determines whether a position is a "large" tile.
     * Cycles every 10 items, positions 2 and 5 are large.
     */
    fun isLargeItem(position: Int): Boolean {
        val mod = position % 10
        return mod == 2 || mod == 5
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLargeItem(position)) TYPE_LARGE else TYPE_SMALL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_explore_grid, parent, false)
        return ExploreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val isLarge = isLargeItem(position)
        
        // Calculate dimensions
        val screenWidth = holder.itemView.context.resources.displayMetrics.widthPixels
        val density = holder.itemView.context.resources.displayMetrics.density
        val spacingPx = (1 * density).toInt() // 1dp spacing
        val smallWidth = (screenWidth - spacingPx * 6) / 3  // 3 columns with spacing
        val smallHeight = smallWidth  // Square tiles
        val largeHeight = smallHeight * 2 + spacingPx * 2  // 2 rows tall
        
        val params = holder.itemView.layoutParams
        params.height = if (isLarge) largeHeight else smallHeight
        holder.itemView.layoutParams = params
        
        holder.bind(getItem(position))
    }

    inner class ExploreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_explore_image)

        fun bind(post: Post) {
            if (post.postImageUrl.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(post.postImageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    imageView.setImageResource(0)
                    imageView.setBackgroundColor(0xFFEFEFEF.toInt())
                }
            } else {
                imageView.setImageResource(0)
                imageView.setBackgroundColor(0xFFEFEFEF.toInt())
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.postId == newItem.postId
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}
