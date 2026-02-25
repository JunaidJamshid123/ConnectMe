package com.junaidjamshid.i211203.presentation.profile

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityUserProfileBinding
import com.junaidjamshid.i211203.presentation.chat.ChatActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture User Profile Activity (for viewing other users).
 */
@AndroidEntryPoint
class UserProfileActivity : AppCompatActivity() {
    
    companion object {
        fun newIntent(context: Context, userId: String): Intent {
            return Intent(context, UserProfileActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
        }
    }
    
    private lateinit var binding: ActivityUserProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    
    private var userId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userId = intent.getStringExtra("USER_ID")
        
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupClickListeners()
        observeUiState()
        
        viewModel.loadUserProfile(userId!!)
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
        // Hide follow/message buttons if viewing own profile
        if (state.isCurrentUser) {
            binding.btnFollow.visibility = View.GONE
            binding.btnMessage.visibility = View.GONE
        }
        
        // Update user info
        state.user?.let { user ->
            binding.userNameText.text = user.username
            
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
            }
        }
        
        // Update counts
        binding.userPostsCount.text = state.postsCount.toString()
        binding.userFollowersCount.text = state.followersCount.toString()
        binding.userFollowingCount.text = state.followingCount.toString()
        
        // Update follow button state
        binding.btnFollow.text = if (state.isFollowing) "Unfollow" else "Follow"
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
}
