package com.junaidjamshid.i211203.presentation.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.common.animation.StaggeredFadeAnimator
import com.junaidjamshid.i211203.presentation.discover.DiscoverPeopleActivity
import com.junaidjamshid.i211203.presentation.home.adapter.HomeFeedAdapter
import com.junaidjamshid.i211203.presentation.home.adapter.HomeFeedItem
import com.junaidjamshid.i211203.presentation.home.adapter.StoryAdapterNew
import com.junaidjamshid.i211203.presentation.home.video.ExoPlayerPool
import com.junaidjamshid.i211203.presentation.home.video.VideoAutoPlayManager
import com.junaidjamshid.i211203.presentation.main.MainActivityNew
import com.junaidjamshid.i211203.presentation.messages.DmsActivity
import com.junaidjamshid.i211203.presentation.notifications.NotificationsActivity
import com.junaidjamshid.i211203.presentation.post.CommentsActivity
import com.junaidjamshid.i211203.presentation.post.PostDetailActivity
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import com.junaidjamshid.i211203.presentation.story.AddStoryActivity
import com.junaidjamshid.i211203.presentation.story.StoryDisplayActivityNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Home Fragment refactored to use Clean Architecture with ViewModel.
 * Supports image posts, carousels, and video/reel posts with auto-play.
 */
@AndroidEntryPoint
class HomeFragmentNew : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var feedAdapter: HomeFeedAdapter
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapterNew
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var emptyStateView: LinearLayout? = null
    private var storiesContainer: LinearLayout? = null
    private var postsContainer: FrameLayout? = null
    private var shimmerStories: ShimmerFrameLayout? = null
    private var shimmerPosts: ShimmerFrameLayout? = null
    private var staggeredAnimator: StaggeredFadeAnimator? = null
    private var notificationBadge: TextView? = null
    
    // Video playback components
    private var playerPool: ExoPlayerPool? = null
    private var videoAutoPlayManager: VideoAutoPlayManager? = null
    
    private val TAG = "HomeFragmentNew"

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        setupViews(view)
        setupRecyclerViews(view)
        setupSwipeRefresh(view)
        observeState()
        
        return view
    }
    
    private fun setupViews(view: View) {
        val addStory = view.findViewById<FrameLayout>(R.id.addStroy)
        val dms = view.findViewById<ImageView>(R.id.DMs)
        val notificationsIcon = view.findViewById<ImageView>(R.id.notifications_icon)
        val notificationContainer = view.findViewById<FrameLayout>(R.id.notification_container)
        notificationBadge = view.findViewById(R.id.notification_badge)
        val currentUserImage = view.findViewById<ImageView>(R.id.current_user_image)
        val logoText = view.findViewById<TextView>(R.id.instagram_logo)
        val nestedScrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.nested_scroll_view)
        emptyStateView = view.findViewById(R.id.empty_state)
        storiesContainer = view.findViewById(R.id.stories_container)
        postsContainer = view.findViewById(R.id.posts_container)
        shimmerStories = view.findViewById(R.id.shimmer_stories)
        shimmerPosts = view.findViewById(R.id.shimmer_posts)

        // Make shimmer post image placeholders square (width = screen width)
        shimmerPosts?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                shimmerPosts?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                val screenWidth = shimmerPosts?.width ?: return
                shimmerPosts?.findViewsWithTag("square_placeholder")?.forEach { placeholder ->
                    placeholder.layoutParams = placeholder.layoutParams.apply {
                        height = screenWidth
                    }
                }
            }
        })

        dms.setOnClickListener {
            // Open DMs Activity
            startActivity(DmsActivity.newIntent(requireContext()))
        }
        
        // Use the container for better tap target
        notificationContainer.setOnClickListener {
            // Open Notifications Activity
            startActivity(NotificationsActivity.newIntent(requireContext()))
        }
        
        addStory.setOnClickListener {
            // Launch dedicated Add Story screen
            startActivity(Intent(requireContext(), AddStoryActivity::class.java))
        }
        
        // Smooth scroll to top when logo is tapped
        logoText?.setOnClickListener {
            nestedScrollView?.smoothScrollTo(0, 0)
            recyclerView.scrollToPosition(0)
        }
    }
    
    private fun setupSwipeRefresh(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh)
        swipeRefreshLayout?.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
        swipeRefreshLayout?.setOnRefreshListener {
            staggeredAnimator?.resetAnimationState()
            viewModel.onRefresh()
        }
    }
    
    private fun setupRecyclerViews(view: View) {
        // Initialize ExoPlayer pool for video playback
        playerPool = ExoPlayerPool(requireContext())
        
        // Posts RecyclerView (now multi-type with suggestions)
        recyclerView = view.findViewById(R.id.recycler_view_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)
        feedAdapter = HomeFeedAdapter(
            onLikeClick = { postId -> viewModel.onLikePost(postId) },
            onCommentClick = { postId -> onCommentClicked(postId) },
            onShareClick = { postId -> onShareClicked(postId) },
            onSaveClick = { postId -> onSaveClicked(postId) },
            onProfileClick = { userId -> onProfileClicked(userId) },
            onMenuClick = { post -> onMenuClicked(post) },
            onFollowClick = { userId -> viewModel.onFollowUser(userId) },
            onSeeAllSuggestionsClick = {
                startActivity(DiscoverPeopleActivity.newIntent(requireContext()))
            },
            onMuteToggle = { 
                // Optional: handle mute toggle feedback
                val muted = videoAutoPlayManager?.isMuted() ?: true
                Toast.makeText(context, if (muted) "Sound off" else "Sound on", Toast.LENGTH_SHORT).show()
            },
            onVideoClick = { postId ->
                // Toggle play/pause on tap
                videoAutoPlayManager?.togglePlayPause(postId)
            }
        )
        feedAdapter.currentUserId = viewModel.currentUserId ?: ""
        feedAdapter.playerPool = playerPool
        recyclerView.adapter = feedAdapter
        
        // Add staggered animation for smooth item appearance
        staggeredAnimator = StaggeredFadeAnimator()
        recyclerView.itemAnimator = staggeredAnimator
        
        // RecyclerView optimizations for smooth scrolling
        recyclerView.setHasFixedSize(false)
        recyclerView.setItemViewCacheSize(10)
        (recyclerView.layoutManager as? LinearLayoutManager)?.initialPrefetchItemCount = 5
        
        // Initialize VideoAutoPlayManager for auto-play on scroll
        videoAutoPlayManager = VideoAutoPlayManager(
            recyclerView = recyclerView,
            playerPool = playerPool!!,
            onVideoViewTracked = { postId ->
                viewModel.trackVideoView(postId)
            }
        )
        videoAutoPlayManager?.setAdapter(feedAdapter) { position ->
            feedAdapter.getPostAtPosition(position)
        }
        
        // Stories RecyclerView
        storiesRecyclerView = view.findViewById(R.id.recycler_view_stories)
        storiesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        storyAdapter = StoryAdapterNew { story ->
            // Launch story viewer for the story's user
            startActivity(
                StoryDisplayActivityNew.newIntent(requireContext(), story.userId, story.storyId)
            )
        }
        storiesRecyclerView.adapter = storyAdapter
    }
    
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
    
    private fun updateUI(state: HomeUiState) {
        // Stop refreshing indicator
        swipeRefreshLayout?.isRefreshing = state.isRefreshing

        // Shimmer for stories
        if (state.isLoadingStories && !state.isRefreshing) {
            shimmerStories?.visibility = View.VISIBLE
            shimmerStories?.startShimmer()
            storiesContainer?.visibility = View.GONE
        } else {
            shimmerStories?.stopShimmer()
            shimmerStories?.visibility = View.GONE
            storiesContainer?.visibility = View.VISIBLE
        }

        // Shimmer for posts
        if (state.isLoadingPosts && !state.isRefreshing) {
            shimmerPosts?.visibility = View.VISIBLE
            shimmerPosts?.startShimmer()
            postsContainer?.visibility = View.GONE
        } else {
            shimmerPosts?.stopShimmer()
            shimmerPosts?.visibility = View.GONE
            postsContainer?.visibility = View.VISIBLE
        }
        
        // Update current user profile image
        state.currentUser?.let { user ->
            user.profilePicture?.let { profilePic ->
                view?.findViewById<ImageView>(R.id.current_user_image)?.let { imageView ->
                    decodeBase64Image(profilePic)?.let { bitmap ->
                        imageView.setImageBitmap(bitmap)
                    }
                }
            }
        }

        // Update "Your Story" section based on whether user has active story
        view?.let { v ->
            val storyRing = v.findViewById<View>(R.id.story_ring)
            val addStoryPlus = v.findViewById<FrameLayout>(R.id.add_story_plus)
            val yourStoryText = v.findViewById<TextView>(R.id.your_story_text)
            
            if (state.currentUserHasStory) {
                // User has active story - show gradient ring, hide plus button
                storyRing?.visibility = View.VISIBLE
                addStoryPlus?.visibility = View.GONE
                yourStoryText?.text = "Your story"
                yourStoryText?.setTextColor(0xFF262626.toInt())
            } else {
                // No active story - hide ring, show plus button
                storyRing?.visibility = View.GONE
                addStoryPlus?.visibility = View.VISIBLE
                yourStoryText?.text = "Your story"
                yourStoryText?.setTextColor(0xFF8E8E8E.toInt())
            }
        }

        // Update following state on feed adapter
        feedAdapter.followingUserIds = state.followingUserIds
        feedAdapter.currentUserId = viewModel.currentUserId ?: ""

        // Build mixed feed: posts + inline suggestions row
        val feedItems = buildFeedItems(state)
        feedAdapter.submitList(feedItems)
        
        // Update stories
        storyAdapter.submitList(state.stories)
        
        // Handle errors
        state.postsError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
        
        state.storiesError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
        
        // Show/hide empty state (only when not loading)
        if (!state.isLoadingPosts && state.posts.isEmpty()) {
            emptyStateView?.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateView?.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        
        // Update notification badge
        updateNotificationBadge(state.unreadNotificationCount)
    }
    
    /**
     * Updates the notification badge visibility and count.
     */
    private fun updateNotificationBadge(count: Int) {
        notificationBadge?.let { badge ->
            if (count > 0) {
                badge.visibility = View.VISIBLE
                badge.text = if (count > 99) "99+" else count.toString()
            } else {
                badge.visibility = View.GONE
            }
        }
    }
    
    /**
     * Build the mixed feed list: posts + suggestion row inserted after every ~5 posts.
     * Only shows suggestions for users not already followed.
     */
    private fun buildFeedItems(state: HomeUiState): List<HomeFeedItem> {
        val items = mutableListOf<HomeFeedItem>()
        val posts = state.posts
        // Filter suggestions: exclude already-followed users
        val unfollowedSuggestions = state.suggestedUsers.filter {
            it.userId !in state.followingUserIds
        }

        val insertAfter = 3 // Insert suggestion row after 3rd post (0-indexed)
        var suggestionsInserted = false

        for ((index, post) in posts.withIndex()) {
            items.add(HomeFeedItem.PostItem(post))

            // Insert suggestions after the 3rd post (or at end if fewer posts)
            if (index == insertAfter && !suggestionsInserted && unfollowedSuggestions.isNotEmpty()) {
                items.add(HomeFeedItem.SuggestionsItem(unfollowedSuggestions.take(10)))
                suggestionsInserted = true
            }
        }

        // If we have fewer posts than insertAfter, still add suggestions at end
        if (!suggestionsInserted && unfollowedSuggestions.isNotEmpty() && posts.isNotEmpty()) {
            items.add(HomeFeedItem.SuggestionsItem(unfollowedSuggestions.take(10)))
        }

        return items
    }

    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64 image: ${e.message}")
            null
        }
    }
    
    private fun onCommentClicked(postId: String) {
        startActivity(CommentsActivity.newIntent(requireContext(), postId))
    }
    
    private fun onShareClicked(postId: String) {
        // Share post functionality
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this post on ConnectMe!")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
    
    private fun onSaveClicked(postId: String) {
        viewModel.onSavePost(postId)
    }
    
    private fun onProfileClicked(userId: String) {
        startActivity(UserProfileActivity.newIntent(requireContext(), userId))
    }
    
    private fun onMenuClicked(post: com.junaidjamshid.i211203.domain.model.Post) {
        // Show post menu options
        Toast.makeText(context, "Menu for post ${post.postId}", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Refresh the feed - can be called from parent activity
     */
    fun refreshFeed() {
        viewModel.onRefresh()
    }
    
    // ========================= VIDEO LIFECYCLE =========================
    
    override fun onResume() {
        super.onResume()
        // Resume video auto-play when fragment becomes visible
        videoAutoPlayManager?.resume()
        // Refresh notification count (e.g., after viewing notifications)
        viewModel.refreshNotificationCount()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause all videos when fragment goes to background
        videoAutoPlayManager?.pauseAll()
    }
    
    override fun onDestroyView() {
        // Clean up video resources
        videoAutoPlayManager?.release()
        videoAutoPlayManager = null
        playerPool?.releaseAll()
        playerPool = null
        super.onDestroyView()
    }
    
    companion object {
        @JvmStatic
        fun newInstance() = HomeFragmentNew()
    }
}

/** Recursively find all child views with the given tag. */
private fun ViewGroup.findViewsWithTag(tag: String): List<View> {
    val result = mutableListOf<View>()
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child.tag == tag) result.add(child)
        if (child is ViewGroup) result.addAll(child.findViewsWithTag(tag))
    }
    return result
}
