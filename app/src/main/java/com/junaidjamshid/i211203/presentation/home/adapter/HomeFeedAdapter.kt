package com.junaidjamshid.i211203.presentation.home.adapter

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Base64
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.presentation.post.adapter.ImageCarouselAdapter

/**
 * Sealed class representing the different items in the home feed.
 */
sealed class HomeFeedItem {
    data class PostItem(val post: Post) : HomeFeedItem()
    data class SuggestionsItem(val suggestions: List<User>) : HomeFeedItem()
}

/**
 * Multi-type adapter for the home feed that handles both posts and suggestion rows.
 * Suggestions appear inline between posts similar to Instagram.
 */
class HomeFeedAdapter(
    private val onLikeClick: (String) -> Unit,
    private val onCommentClick: (String) -> Unit,
    private val onShareClick: (String) -> Unit,
    private val onSaveClick: (String) -> Unit,
    private val onProfileClick: (String) -> Unit,
    private val onMenuClick: (Post) -> Unit,
    private val onFollowClick: (String) -> Unit,
    private val onSeeAllSuggestionsClick: () -> Unit
) : ListAdapter<HomeFeedItem, RecyclerView.ViewHolder>(FeedDiffCallback()) {

    companion object {
        private const val TYPE_POST = 0
        private const val TYPE_SUGGESTIONS = 1
    }

    /** Set of user IDs the current user is following */
    var followingUserIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /** Current user ID to hide follow button on own posts */
    var currentUserId: String = ""

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeFeedItem.PostItem -> TYPE_POST
            is HomeFeedItem.SuggestionsItem -> TYPE_SUGGESTIONS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_post, parent, false)
                PostViewHolder(view)
            }
            TYPE_SUGGESTIONS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_suggestions_row, parent, false)
                SuggestionsViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeFeedItem.PostItem -> (holder as PostViewHolder).bind(item.post)
            is HomeFeedItem.SuggestionsItem -> (holder as SuggestionsViewHolder).bind(item.suggestions)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PostViewHolder) holder.cleanup()
    }

    // ========================= POST VIEW HOLDER =========================

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.username_text)
        private val followButton: TextView = itemView.findViewById(R.id.btn_follow_user)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitle_text)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val postImageContainer: View = itemView.findViewById(R.id.post_image_container)
        private val doubleTapHeart: ImageView = itemView.findViewById(R.id.double_tap_heart)
        private val postCarousel: ViewPager2 = itemView.findViewById(R.id.post_carousel)
        private val carouselCounter: TextView = itemView.findViewById(R.id.carousel_counter)
        private val carouselDots: LinearLayout = itemView.findViewById(R.id.carousel_dots)
        private val heartButton: ImageView = itemView.findViewById(R.id.heart)
        private val commentButton: ImageView = itemView.findViewById(R.id.comment)
        private val shareButton: ImageView = itemView.findViewById(R.id.send)
        private val saveButton: ImageView = itemView.findViewById(R.id.save)
        private val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        private val postCaption: TextView = itemView.findViewById(R.id.post_caption)
        private val viewComments: TextView = itemView.findViewById(R.id.view_comments)
        private val addCommentRow: View = itemView.findViewById(R.id.add_comment_row)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val menuButton: ImageView = itemView.findViewById(R.id.menu_dots)

        private var carouselAdapter: ImageCarouselAdapter? = null
        private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
        private val subtitleHandler = Handler(Looper.getMainLooper())
        private var subtitleRunnable: Runnable? = null

        private var currentPostId: String = ""

        // Gesture detector for double-tap to like
        private val gestureDetector = GestureDetectorCompat(itemView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onLikeClick(currentPostId)
                    showDoubleTapLikeAnimation()
                    return true
                }
            }
        )

        private fun showDoubleTapLikeAnimation() {
            val fadeIn = AnimationUtils.loadAnimation(itemView.context, R.anim.like_heart_in)
            val fadeOut = AnimationUtils.loadAnimation(itemView.context, R.anim.like_heart_out)

            doubleTapHeart.alpha = 1f
            doubleTapHeart.startAnimation(fadeIn)

            doubleTapHeart.postDelayed({
                doubleTapHeart.startAnimation(fadeOut)
                fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        doubleTapHeart.alpha = 0f
                    }
                })
            }, 500)
        }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(post: Post) {
            currentPostId = post.postId
            usernameText.text = post.username
            likesCount.text = "${post.likesCount} likes"
            timestamp.text = getTimeAgo(post.timestamp)

            // Setup double-tap gesture on post image container
            postImageContainer.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            // Follow button logic — show "Follow" for users not followed, hide for self or already followed
            val isOwnPost = post.userId == currentUserId
            val isFollowing = followingUserIds.contains(post.userId)
            if (!isOwnPost && !isFollowing) {
                followButton.visibility = View.VISIBLE
                followButton.text = "Follow"
                followButton.setTextColor(0xFF0095F6.toInt())
                followButton.setOnClickListener { onFollowClick(post.userId) }
            } else {
                followButton.visibility = View.GONE
            }

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

            // View all comments
            if (post.commentsCount > 0) {
                viewComments.visibility = View.VISIBLE
                viewComments.text = if (post.commentsCount == 1) {
                    "View 1 comment"
                } else {
                    "View all ${post.commentsCount} comments"
                }
            } else {
                viewComments.visibility = View.GONE
            }

            // Click listeners
            heartButton.setOnClickListener { onLikeClick(post.postId) }
            commentButton.setOnClickListener { onCommentClick(post.postId) }
            shareButton.setOnClickListener { onShareClick(post.postId) }
            saveButton.setOnClickListener { onSaveClick(post.postId) }
            profileImage.setOnClickListener { onProfileClick(post.userId) }
            usernameText.setOnClickListener { onProfileClick(post.userId) }
            menuButton.setOnClickListener { onMenuClick(post) }
            viewComments.setOnClickListener { onCommentClick(post.postId) }
            addCommentRow.setOnClickListener { onCommentClick(post.postId) }
        }

        private fun setupSubtitle(post: Post) {
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

            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }

            if (carouselAdapter == null) {
                carouselAdapter = ImageCarouselAdapter()
                postCarousel.adapter = carouselAdapter
            }

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

        fun cleanup() {
            subtitleRunnable?.let { subtitleHandler.removeCallbacks(it) }
            subtitleRunnable = null
            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }
            pageChangeCallback = null
        }
    }

    // ========================= SUGGESTIONS VIEW HOLDER =========================

    inner class SuggestionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val suggestionsRecyclerView: RecyclerView = itemView.findViewById(R.id.suggestions_recycler)
        private val seeAllButton: TextView = itemView.findViewById(R.id.btn_see_all)

        fun bind(suggestions: List<User>) {
            val adapter = SuggestionCardAdapter(
                onFollowClick = { userId -> onFollowClick(userId) },
                onDismissClick = { /* handled by ViewModel later */ },
                onProfileClick = { userId -> onProfileClick(userId) }
            )
            adapter.followingUserIds = followingUserIds
            suggestionsRecyclerView.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            suggestionsRecyclerView.adapter = adapter
            adapter.submitList(suggestions)

            seeAllButton.setOnClickListener { onSeeAllSuggestionsClick() }
        }
    }

    // ========================= HELPERS =========================

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

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7

        return when {
            weeks > 0 -> "$weeks weeks"
            days > 0 -> "$days days"
            hours > 0 -> "$hours hours"
            minutes > 0 -> "$minutes minutes"
            else -> "now"
        }
    }

    private fun Int.dpToPx(): Int =
        (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

    // ========================= DIFF CALLBACK =========================

    class FeedDiffCallback : DiffUtil.ItemCallback<HomeFeedItem>() {
        override fun areItemsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
            return when {
                oldItem is HomeFeedItem.PostItem && newItem is HomeFeedItem.PostItem ->
                    oldItem.post.postId == newItem.post.postId
                oldItem is HomeFeedItem.SuggestionsItem && newItem is HomeFeedItem.SuggestionsItem -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
            return oldItem == newItem
        }
    }
}
