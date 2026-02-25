package com.junaidjamshid.i211203.presentation.follow

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.databinding.ActivityFollowersBinding
import com.junaidjamshid.i211203.presentation.follow.adapter.FollowAdapterNew
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture Followers Activity.
 */
@AndroidEntryPoint
class FollowersActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFollowersBinding
    private val viewModel: FollowViewModel by viewModels()
    
    private lateinit var followAdapter: FollowAdapterNew
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityFollowersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val userId = intent.getStringExtra("USER_ID")
        
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        
        viewModel.loadFollowers(userId)
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
            showRemoveButton = true
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
        
        binding.dms.text = "${state.users.size} followers"
        
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
}
