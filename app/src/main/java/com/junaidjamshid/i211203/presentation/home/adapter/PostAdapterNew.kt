package com.junaidjamshid.i211203.presentation.home.adapter

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.presentation.post.adapter.ImageCarouselAdapter

/**
 * Instagram-style Post Adapter with carousel, location, and music support.
 * Location and music alternate in the subtitle below the username.
 */
class PostAdapterNew(
    private val onLikeClick: (String) -> Unit,
    private val onCommentClick: (String) -> Unit,
    private val onShareClick: (String) -> Unit,
    private val onSaveClick: (String) -> Unit,
    private val onProfileClick: (String) -> Unit,
    private val onMenuClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapterNew.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.username_text)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitle_text)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val postCarousel: ViewPager2 = itemView.findViewById(R.id.post_carousel)
        private val carouselCounter: TextView = itemView.findViewById(R.id.carousel_counter)
        private val carouselDots: LinearLayout = itemView.findViewById(R.id.carousel_dots)
        private val heartButton: ImageView = itemView.findViewById(R.id.heart)
        private val commentButton: ImageView = itemView.findViewById(R.id.comment)
        private val shareButton: ImageView = itemView.findViewById(R.id.send)
        private val saveButton: ImageView = itemView.findViewById(R.id.save)
        private val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        private val postCaption: TextView = itemView.findViewById(R.id.post_caption)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val menuButton: ImageView = itemView.findViewById(R.id.menu_dots)

        private var carouselAdapter: ImageCarouselAdapter? = null
        private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
        private val subtitleHandler = Handler(Looper.getMainLooper())
        private var subtitleRunnable: Runnable? = null

        fun bind(post: Post) {
            usernameText.text = post.username
            likesCount.text = "${post.likesCount} likes"
            timestamp.text = getTimeAgo(post.timestamp)

            // Caption with bold username
            if (post.caption.isNotEmpty()) {
                val ssb = SpannableStringBuilder()
                ssb.append(post.username, StyleSpan(android.graphics.Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.append(" ")
                ssb.append(post.caption)
                postCaption.text = ssb
                postCaption.visibility = View.VISIBLE
            } else {
                postCaption.visibility = View.GONE
            }

            // Subtitle: show location, music, or alternate both
            setupSubtitle(post)

            // Profile image
            loadBase64Image(post.userProfileImage, profileImage, R.drawable.default_profile)

            // Image display: carousel vs single
            val allImages = post.allImages
            if (allImages.size > 1) {
                setupCarousel(allImages)
            } else {
                setupSingleImage(allImages, post.postImageUrl)
            }

            // Like state
            heartButton.setImageResource(
                if (post.isLikedByCurrentUser) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )

            // Click listeners
            heartButton.setOnClickListener { onLikeClick(post.postId) }
            commentButton.setOnClickListener { onCommentClick(post.postId) }
            shareButton.setOnClickListener { onShareClick(post.postId) }
            saveButton.setOnClickListener { onSaveClick(post.postId) }
            profileImage.setOnClickListener { onProfileClick(post.userId) }
            usernameText.setOnClickListener { onProfileClick(post.userId) }
            menuButton.setOnClickListener { onMenuClick(post) }
        }

        /**
         * Shows location and/or music below username, alternating if both exist.
         * Exactly like Instagram: location for a few seconds, then music, cycling.
         */
        private fun setupSubtitle(post: Post) {
            // Stop any previous animation
            subtitleRunnable?.let { subtitleHandler.removeCallbacks(it) }
            subtitleRunnable = null

            val hasLoc = post.hasLocation
            val hasMus = post.hasMusic

            if (!hasLoc && !hasMus) {
                subtitleText.visibility = View.GONE
                return
            }

            subtitleText.visibility = View.VISIBLE

            if (hasLoc && hasMus) {
                // Alternate between location and music every 3 seconds
                var showingLocation = true
                subtitleText.text = post.location

                subtitleRunnable = object : Runnable {
                    override fun run() {
                        showingLocation = !showingLocation
                        if (showingLocation) {
                            subtitleText.text = post.location
                        } else {
                            subtitleText.text = "\u266B ${post.musicName} · ${post.musicArtist}"
                        }
                        subtitleHandler.postDelayed(this, 10000)
                    }
                }
                subtitleHandler.postDelayed(subtitleRunnable!!, 10000)
            } else if (hasLoc) {
                subtitleText.text = post.location
            } else {
                subtitleText.text = "\u266B ${post.musicName} · ${post.musicArtist}"
            }
        }

        private fun setupCarousel(images: List<String>) {
            postImage.visibility = View.GONE
            postCarousel.visibility = View.VISIBLE
            carouselCounter.visibility = View.VISIBLE

            // Remove old callback
            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }

            if (carouselAdapter == null) {
                carouselAdapter = ImageCarouselAdapter()
                postCarousel.adapter = carouselAdapter
            }

            // Fix for ViewPager2 inside RecyclerView - reduce sensitivity
            postCarousel.getChildAt(0)?.let { innerRecycler ->
                if (innerRecycler is RecyclerView) {
                    innerRecycler.isNestedScrollingEnabled = false
                    innerRecycler.overScrollMode = View.OVER_SCROLL_NEVER
                }
            }

            carouselAdapter?.submitBase64Images(images)
            postCarousel.setCurrentItem(0, false)

            carouselCounter.text = "1/${images.size}"
            setupDots(images.size)

            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    carouselCounter.text = "${position + 1}/${images.size}"
                    updateDots(position)
                }
            }
            postCarousel.registerOnPageChangeCallback(pageChangeCallback!!)
        }

        private fun setupSingleImage(allImages: List<String>, fallbackUrl: String) {
            postCarousel.visibility = View.GONE
            carouselCounter.visibility = View.GONE
            carouselDots.visibility = View.GONE
            postImage.visibility = View.VISIBLE

            val imageData = if (allImages.isNotEmpty()) allImages[0] else fallbackUrl
            loadBase64Image(imageData, postImage, 0)
        }

        private fun setupDots(count: Int) {
            carouselDots.removeAllViews()
            if (count <= 1) {
                carouselDots.visibility = View.GONE
                return
            }
            carouselDots.visibility = View.VISIBLE
            for (i in 0 until count) {
                val dot = ImageView(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(6.dpToPx(), 6.dpToPx()).apply {
                        marginStart = 3.dpToPx()
                        marginEnd = 3.dpToPx()
                    }
                    setImageResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
                }
                carouselDots.addView(dot)
            }
        }

        private fun updateDots(selectedPos: Int) {
            for (i in 0 until carouselDots.childCount) {
                (carouselDots.getChildAt(i) as? ImageView)?.setImageResource(
                    if (i == selectedPos) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
        }

        private fun loadBase64Image(base64: String, imageView: ImageView, fallbackRes: Int) {
            if (base64.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        return
                    }
                } catch (_: Exception) { }
            }
            if (fallbackRes != 0) imageView.setImageResource(fallbackRes)
        }

        private fun Int.dpToPx(): Int =
            (this * itemView.context.resources.displayMetrics.density).toInt()

        private fun getTimeAgo(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7

            return when {
                weeks > 0 -> "${weeks} weeks"
                days > 0 -> "${days} days"
                hours > 0 -> "${hours} hours"
                minutes > 0 -> "${minutes} minutes"
                else -> "now"
            }
        }

        fun cleanup() {
            subtitleRunnable?.let { subtitleHandler.removeCallbacks(it) }
            subtitleRunnable = null
            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }
            pageChangeCallback = null
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) =
            oldItem.postId == newItem.postId

        override fun areContentsTheSame(oldItem: Post, newItem: Post) =
            oldItem == newItem
    }
}
