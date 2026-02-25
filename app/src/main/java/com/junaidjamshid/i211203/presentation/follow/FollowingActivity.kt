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
import com.junaidjamshid.i211203.databinding.ActivityFollowingBinding
import com.junaidjamshid.i211203.presentation.follow.adapter.FollowAdapterNew
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture Following Activity.
 */
@AndroidEntryPoint
class FollowingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFollowingBinding
    private val viewModel: FollowViewModel by viewModels()
    
    private lateinit var followAdapter: FollowAdapterNew
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityFollowingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val userId = intent.getStringExtra("USER_ID")
        
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        
        viewModel.loadFollowing(userId)
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
            showRemoveButton = false
        )
        
        binding.followingRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FollowingActivity)
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
        
        binding.requests.text = "${state.users.size} following"
        
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
}
