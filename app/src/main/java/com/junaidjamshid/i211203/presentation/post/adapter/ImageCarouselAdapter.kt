package com.junaidjamshid.i211203.presentation.post.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R

/**
 * Adapter for ViewPager2 image carousel.
 * Supports both Bitmap (for create post preview) and Base64 strings (for feed display).
 */
class ImageCarouselAdapter : RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>() {

    private val bitmaps = mutableListOf<Bitmap>()
    private val base64Images = mutableListOf<String>()
    private var mode = Mode.BITMAP

    enum class Mode { BITMAP, BASE64 }

    /**
     * Submit bitmaps directly (used in create post preview)
     */
    fun submitBitmaps(images: List<Bitmap>) {
        mode = Mode.BITMAP
        bitmaps.clear()
        bitmaps.addAll(images)
        base64Images.clear()
        notifyDataSetChanged()
    }

    /**
     * Submit Base64-encoded images (used in feed display)
     */
    fun submitBase64Images(images: List<String>) {
        mode = Mode.BASE64
        base64Images.clear()
        base64Images.addAll(images)
        bitmaps.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        when (mode) {
            Mode.BITMAP -> holder.bindBitmap(bitmaps[position])
            Mode.BASE64 -> holder.bindBase64(base64Images[position])
        }
    }

    override fun getItemCount(): Int = when (mode) {
        Mode.BITMAP -> bitmaps.size
        Mode.BASE64 -> base64Images.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_carousel_image)

        fun bindBitmap(bitmap: Bitmap) {
            imageView.setImageBitmap(bitmap)
        }

        fun bindBase64(base64: String) {
            try {
                val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Failed to decode
            }
        }
    }
}
