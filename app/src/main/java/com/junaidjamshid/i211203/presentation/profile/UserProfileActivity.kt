package com.junaidjamshid.i211203.presentation.profile

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityUserProfileBinding
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.presentation.chat.ChatActivity
import com.junaidjamshid.i211203.presentation.follow.FollowersActivity
import com.junaidjamshid.i211203.presentation.follow.FollowingActivity
import com.junaidjamshid.i211203.presentation.main.MainActivityNew
import com.junaidjamshid.i211203.presentation.post.PostDetailActivity
import com.junaidjamshid.i211203.presentation.profile.adapter.PostGridAdapterNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Instagram-style User Profile Activity (for viewing other users).
 * Pixel-perfect implementation matching Instagram's design language.
 */
@AndroidEntryPoint
class UserProfileActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_NAVIGATE_TO_OWN_PROFILE = "NAVIGATE_TO_OWN_PROFILE"
        
        fun newIntent(context: Context, userId: String): Intent {
            return Intent(context, UserProfileActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
        }
        
        private const val TAB_POSTS = 0
        private const val TAB_REELS = 1
        private const val TAB_TAGGED = 2
    }
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    private lateinit var binding: ActivityUserProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    
    private var userId: String? = null
    private var currentTab = TAB_POSTS
    
    // Adapters for grid views
    private lateinit var postsAdapter: PostGridAdapterNew
    private lateinit var reelsAdapter: PostGridAdapterNew

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSystemUI()
        
        userId = intent.getStringExtra("USER_ID")
        
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Check if user is viewing their own profile - redirect to main profile tab
        val currentUserId = authRepository.getCurrentUserId()
        if (userId == currentUserId) {
            navigateToOwnProfile()
            return
        }
        
        setupAdapters()
        setupRecyclerViews()
        setupClickListeners()
        setupTabs()
        observeUiState()
        
        viewModel.loadUserProfile(userId!!)
    }
    
    private fun navigateToOwnProfile() {
        // Navigate to main activity with profile tab selected
        val intent = Intent(this, MainActivityNew::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAVIGATE_TO_OWN_PROFILE, true)
        }
        startActivity(intent)
        finish()
    }
    
    private fun setupSystemUI() {
        // Make status bar white with dark icons (Instagram style)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    
    private fun setupAdapters() {
        postsAdapter = PostGridAdapterNew { post ->
            // Navigate to post detail
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            startActivity(intent)
        }
        
        reelsAdapter = PostGridAdapterNew { post ->
            // Navigate to post/reel detail
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            startActivity(intent)
        }
    }
    
    private fun setupRecyclerViews() {
        // Posts grid (3 columns, Instagram style)
        binding.postsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserProfileActivity, 3)
            adapter = postsAdapter
            setHasFixedSize(false)
        }
        
        // Reels grid (3 columns)
        binding.reelsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserProfileActivity, 3)
            adapter = reelsAdapter
            setHasFixedSize(false)
        }
        
        // Tagged posts grid (placeholder for now)
        binding.taggedRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserProfileActivity, 3)
            setHasFixedSize(false)
        }
    }
    
    private fun setupTabs() {
        binding.tabGrid.setOnClickListener { switchTab(TAB_POSTS) }
        binding.tabReels.setOnClickListener { switchTab(TAB_REELS) }
        binding.tabTagged.setOnClickListener { switchTab(TAB_TAGGED) }
        
        // Start with Posts tab
        switchTab(TAB_POSTS)
    }
    
    private fun switchTab(tab: Int) {
        currentTab = tab
        
        // Reset all tab indicators and icons
        binding.tabGridIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.tabReelsIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.tabTaggedIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        binding.tabGridIcon.setColorFilter(ContextCompat.getColor(this, R.color.instagram_gray))
        binding.tabReelsIcon.setColorFilter(ContextCompat.getColor(this, R.color.instagram_gray))
        binding.tabTaggedIcon.setColorFilter(ContextCompat.getColor(this, R.color.instagram_gray))
        
        // Hide all recycler views
        binding.postsRecyclerView.visibility = View.GONE
        binding.reelsRecyclerView.visibility = View.GONE
        binding.taggedRecyclerView.visibility = View.GONE
        binding.emptyPostsView.visibility = View.GONE
        
        // Activate selected tab
        when (tab) {
            TAB_POSTS -> {
                binding.tabGridIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.instagram_dark))
                binding.tabGridIcon.setColorFilter(ContextCompat.getColor(this, R.color.instagram_dark))
                binding.postsRecyclerView.visibility = View.VISIBLE
            }
            TAB_REELS -> {
                binding.tabReelsIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.instagram_dark))
                binding.tabReelsIcon.setColorFilter(ContextCompat.getColor(this, R.color.instagram_dark))
                binding.reelsRecyclerView.visibility = View.VISIBLE
            }
            TAB_TAGGED -> {
                binding.tabTaggedIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.instagram_dark))
                binding.tabTaggedIcon.setColorFilter(ContextCompat.getColor(this, R.color.instagram_dark))
                binding.taggedRecyclerView.visibility = View.VISIBLE
            }
        }
        
        // Update empty state based on current state data
        updateEmptyState()
    }
    
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.btnFollow.setOnClickListener {
            userId?.let { viewModel.toggleFollow(it) }
        }
        
        binding.btnMessage.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
        
        // Followers click - navigate to followers page (the whole container)
        binding.userFollowers.setOnClickListener {
            navigateToFollowers()
        }
        
        // Following click - navigate to following page (the whole container)
        binding.userFollowing.setOnClickListener {
            navigateToFollowing()
        }
        
        // Profile picture click (view stories if available)
        binding.profileImageContainer.setOnClickListener {
            // Could implement story viewing here
        }
        
        // More options menu
        binding.moreOptions.setOnClickListener {
            // Show options menu (report, block, etc.)
        }
        
        // Add person / suggest people button
        binding.btnAddPerson.setOnClickListener {
            // Show similar accounts
        }
    }
    
    private fun navigateToFollowers() {
        val intent = Intent(this, FollowersActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("USERNAME", viewModel.uiState.value.user?.username ?: "")
        }
        startActivity(intent)
    }
    
    private fun navigateToFollowing() {
        val intent = Intent(this, FollowingActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("USERNAME", viewModel.uiState.value.user?.username ?: "")
        }
        startActivity(intent)
    }
    
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }
    
    private fun handleUiState(state: ProfileUiState) {
        // Handle loading state with shimmer
        if (state.isLoading) {
            binding.shimmerProfile.visibility = View.VISIBLE
            binding.shimmerProfile.startShimmer()
            binding.profileContent.visibility = View.GONE
            return
        } else {
            binding.shimmerProfile.stopShimmer()
            binding.shimmerProfile.visibility = View.GONE
            binding.profileContent.visibility = View.VISIBLE
        }
        
        // Hide follow/message buttons if viewing own profile
        if (state.isCurrentUser) {
            binding.actionButtonsContainer.visibility = View.GONE
        } else {
            binding.actionButtonsContainer.visibility = View.VISIBLE
        }
        
        // Show/hide story ring based on active stories
        if (state.hasActiveStories) {
            binding.storyRing.visibility = View.VISIBLE
            binding.whiteRing.visibility = View.VISIBLE
            // Resize profile image to fit inside story ring
            val params = binding.userProfileImage.layoutParams
            params.width = resources.getDimensionPixelSize(R.dimen.profile_image_with_story)
            params.height = resources.getDimensionPixelSize(R.dimen.profile_image_with_story)
            binding.userProfileImage.layoutParams = params
        } else {
            binding.storyRing.visibility = View.GONE
            binding.whiteRing.visibility = View.GONE
        }
        
        // Update user info
        state.user?.let { user ->
            // Username in toolbar
            binding.tvUsernameToolbar.text = user.username
            
            // Display name (show fullName if available, otherwise username)
            val displayName = user.fullName.ifEmpty { user.username }
            binding.userNameText.text = displayName
            
            // Bio
            if (user.bio.isNotEmpty()) {
                binding.userBioText.text = user.bio
                binding.userBioText.visibility = View.VISIBLE
            } else {
                binding.userBioText.visibility = View.GONE
            }
            
            // Load profile image
            if (!user.profilePicture.isNullOrEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(user.profilePicture, android.util.Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.userProfileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.userProfileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                binding.userProfileImage.setImageResource(R.drawable.default_profile)
            }
        }
        
        // Update counts
        binding.userPostsCount.text = formatCount(state.postsCount)
        binding.userFollowersCount.text = formatCount(state.followersCount)
        binding.userFollowingCount.text = formatCount(state.followingCount)
        
        // Update follow button style (Instagram style)
        updateFollowButton(state.isFollowing)
        
        // Update posts grids
        postsAdapter.submitList(state.imagePosts)
        reelsAdapter.submitList(state.reelPosts)
        
        // Update empty state visibility based on current tab and actual data
        updateEmptyState(state)
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun updateFollowButton(isFollowing: Boolean) {
        if (isFollowing) {
            // Following state - gray button with dark text (Instagram style)
            binding.btnFollow.text = "Following"
            binding.btnFollow.setBackgroundResource(R.drawable.bg_following_button)
            binding.btnFollow.setTextColor(ContextCompat.getColor(this, R.color.instagram_dark))
        } else {
            // Not following - blue button with white text (Instagram style)
            binding.btnFollow.text = "Follow"
            binding.btnFollow.setBackgroundResource(R.drawable.bg_follow_button)
            binding.btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }
    
    private fun updateEmptyState(state: ProfileUiState? = null) {
        val uiState = state ?: viewModel.uiState.value
        
        // Don't show empty state while loading
        if (uiState.isLoading) {
            binding.emptyPostsView.visibility = View.GONE
            return
        }
        
        binding.emptyPostsView.visibility = when (currentTab) {
            TAB_POSTS -> if (uiState.imagePosts.isEmpty()) View.VISIBLE else View.GONE
            TAB_REELS -> if (uiState.reelPosts.isEmpty()) View.VISIBLE else View.GONE
            TAB_TAGGED -> View.VISIBLE // Tagged always empty for now
            else -> View.GONE
        }
    }
    
    /**
     * Format count like Instagram (e.g., 1.2K, 5.5M)
     */
    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 10_000 -> String.format("%.1fK", count / 1_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}
