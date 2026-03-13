package com.junaidjamshid.i211203.presentation.follow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityFollowersNewBinding
import com.junaidjamshid.i211203.presentation.follow.adapter.FollowAdapterNew
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style Followers/Following Activity with tabs.
 */
@AndroidEntryPoint
class FollowersActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFollowersNewBinding
    private val viewModel: FollowViewModel by viewModels()
    
    private lateinit var followAdapter: FollowAdapterNew
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityFollowersNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val userId = intent.getStringExtra("USER_ID")
        val username = intent.getStringExtra("USERNAME") ?: ""
        val initialTab = intent.getStringExtra("TAB")?.let { 
            if (it == "following") FollowTab.FOLLOWING else FollowTab.FOLLOWERS 
        } ?: FollowTab.FOLLOWERS
        
        binding.userName.text = username
        
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        
        viewModel.initialize(userId, initialTab)
    }
    
    private fun setupRecyclerView() {
        followAdapter = FollowAdapterNew(
            onUserClick = { user ->
                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("USER_ID", user.user.userId)
                startActivity(intent)
            },
            onFollowClick = { user ->
                viewModel.toggleFollow(user.user.userId)
            },
            onRemoveClick = { user ->
                viewModel.removeFollower(user.user.userId)
            },
            showRemoveButton = true,
            isCurrentUserProfile = true,
            currentUserId = viewModel.getLoggedInUserId()
        )
        
        binding.followersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FollowersActivity)
            adapter = followAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.back.setOnClickListener {
            finish()
        }
        
        binding.tabFollowers.setOnClickListener {
            viewModel.switchTab(FollowTab.FOLLOWERS)
        }
        
        binding.tabFollowing.setOnClickListener {
            viewModel.switchTab(FollowTab.FOLLOWING)
        }
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
    
    private fun handleUiState(state: FollowUiState) {
        // Handle shimmer loading
        if (state.isLoading && state.users.isEmpty()) {
            binding.shimmerContainer.visibility = View.VISIBLE
            binding.shimmerContainer.startShimmer()
            binding.followersRecyclerView.visibility = View.GONE
            binding.emptyStateView.visibility = View.GONE
        } else {
            binding.shimmerContainer.stopShimmer()
            binding.shimmerContainer.visibility = View.GONE
            
            followAdapter.submitList(state.users)
            
            // Show/hide empty state
            if (state.users.isEmpty()) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.followersRecyclerView.visibility = View.GONE
                
                // Update empty state text based on tab
                when (state.currentTab) {
                    FollowTab.FOLLOWERS -> {
                        binding.emptyTitle.text = getString(R.string.no_followers_yet)
                        binding.emptySubtitle.text = getString(R.string.followers_will_appear_here)
                    }
                    FollowTab.FOLLOWING -> {
                        binding.emptyTitle.text = getString(R.string.no_following_yet)
                        binding.emptySubtitle.text = getString(R.string.following_will_appear_here)
                    }
                }
            } else {
                binding.emptyStateView.visibility = View.GONE
                binding.followersRecyclerView.visibility = View.VISIBLE
            }
        }
        
        // Update counts
        binding.followersCount.text = state.followersCount.toString()
        binding.followingCount.text = state.followingCount.toString()
        
        // Update tab selection UI
        updateTabSelection(state.currentTab)
        
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun updateTabSelection(currentTab: FollowTab) {
        val activeColor = ContextCompat.getColor(this, R.color.black)
        val inactiveColor = ContextCompat.getColor(this, R.color.instagram_text_gray)
        
        when (currentTab) {
            FollowTab.FOLLOWERS -> {
                // Followers tab active
                binding.followersCount.setTextColor(activeColor)
                (binding.tabFollowers.getChildAt(1) as? TextView)?.setTextColor(activeColor)
                
                binding.followingCount.setTextColor(inactiveColor)
                (binding.tabFollowing.getChildAt(1) as? TextView)?.setTextColor(inactiveColor)
                
                // Animate indicator to left
                binding.tabIndicator.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .start()
            }
            FollowTab.FOLLOWING -> {
                // Following tab active
                binding.followersCount.setTextColor(inactiveColor)
                (binding.tabFollowers.getChildAt(1) as? TextView)?.setTextColor(inactiveColor)
                
                binding.followingCount.setTextColor(activeColor)
                (binding.tabFollowing.getChildAt(1) as? TextView)?.setTextColor(activeColor)
                
                // Animate indicator to right
                val indicatorWidth = binding.tabIndicator.width.toFloat()
                binding.tabIndicator.animate()
                    .translationX(indicatorWidth)
                    .setDuration(200)
                    .start()
            }
        }
    }
}
