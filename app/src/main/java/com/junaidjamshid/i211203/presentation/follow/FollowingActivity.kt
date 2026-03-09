package com.junaidjamshid.i211203.presentation.follow

import android.content.Intent
import android.os.Bundle
import android.view.View
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
 * Instagram-style Following Activity - uses same layout as Followers with Following tab selected.
 */
@AndroidEntryPoint
class FollowingActivity : AppCompatActivity() {
    
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
        
        binding.userName.text = username
        
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        
        // Initialize with Following tab selected
        viewModel.initialize(userId, FollowTab.FOLLOWING)
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
            onRemoveClick = null,
            showRemoveButton = false,
            isCurrentUserProfile = false
        )
        
        binding.followersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FollowingActivity)
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
        followAdapter.submitList(state.users)
        
        // Update counts
        binding.followersCount.text = state.followersCount.toString()
        binding.followingCount.text = state.followingCount.toString()
        
        // Update tab selection UI
        updateTabSelection(state.currentTab)
        
        // Show/hide empty state
        if (state.users.isEmpty() && !state.isLoading) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.followersRecyclerView.visibility = View.GONE
            
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
                binding.followersCount.setTextColor(activeColor)
                (binding.tabFollowers.getChildAt(1) as? TextView)?.setTextColor(activeColor)
                
                binding.followingCount.setTextColor(inactiveColor)
                (binding.tabFollowing.getChildAt(1) as? TextView)?.setTextColor(inactiveColor)
                
                binding.tabIndicator.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .start()
            }
            FollowTab.FOLLOWING -> {
                binding.followersCount.setTextColor(inactiveColor)
                (binding.tabFollowers.getChildAt(1) as? TextView)?.setTextColor(inactiveColor)
                
                binding.followingCount.setTextColor(activeColor)
                (binding.tabFollowing.getChildAt(1) as? TextView)?.setTextColor(activeColor)
                
                val indicatorWidth = binding.tabIndicator.width.toFloat()
                binding.tabIndicator.animate()
                    .translationX(indicatorWidth)
                    .setDuration(200)
                    .start()
            }
        }
    }
}
