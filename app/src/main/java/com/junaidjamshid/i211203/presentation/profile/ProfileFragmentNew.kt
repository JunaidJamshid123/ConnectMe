package com.junaidjamshid.i211203.presentation.profile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.FragmentProfileBinding
import com.junaidjamshid.i211203.presentation.auth.LoginActivity
import com.junaidjamshid.i211203.presentation.follow.FollowersActivity
import com.junaidjamshid.i211203.presentation.follow.FollowingActivity
import com.junaidjamshid.i211203.presentation.profile.adapter.PostGridAdapterNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture Profile Fragment.
 */
@AndroidEntryPoint
class ProfileFragmentNew : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var postGridAdapter: PostGridAdapterNew
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        
        viewModel.loadCurrentUserProfile()
    }
    
    private fun setupRecyclerView() {
        postGridAdapter = PostGridAdapterNew { post ->
            // Navigate to post detail
        }
        
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = postGridAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.editProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        
        binding.logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        
        binding.followers.setOnClickListener {
            startActivity(Intent(requireContext(), FollowersActivity::class.java))
        }
        
        binding.following.setOnClickListener {
            startActivity(Intent(requireContext(), FollowingActivity::class.java))
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }
    
    private fun handleUiState(state: ProfileUiState) {
        // Update user info
        state.user?.let { user ->
            binding.usernameText.text = user.username
            binding.bioText.text = user.bio.ifEmpty { "No bio yet" }
            
            // Load profile image
            if (!user.profilePicture.isNullOrEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(user.profilePicture, android.util.Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.profileImage.setImageResource(R.drawable.junaid1)
                }
            }
        }
        
        // Update counts
        binding.postsCount.text = state.postsCount.toString()
        binding.followersCount.text = state.followersCount.toString()
        binding.followingCount.text = state.followingCount.toString()
        
        // Update posts grid
        postGridAdapter.submitList(state.posts)
        
        // Handle logout success
        if (state.logoutSuccess) {
            navigateToLogin()
        }
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                viewModel.logout()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
