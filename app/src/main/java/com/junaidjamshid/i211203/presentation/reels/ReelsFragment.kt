package com.junaidjamshid.i211203.presentation.reels

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.FragmentReelsBinding
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.presentation.post.CommentsActivity
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import com.junaidjamshid.i211203.presentation.reels.adapter.ReelsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style Reels Fragment - Full screen vertical video feed.
 * Swipe vertically to browse reels with auto-play.
 */
@AndroidEntryPoint
class ReelsFragment : Fragment() {

    private var _binding: FragmentReelsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReelsViewModel by viewModels()
    private lateinit var reelsAdapter: ReelsAdapter

    private var currentPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupReelsViewPager()
        setupClickListeners()
        observeUiState()
    }

    private fun setupReelsViewPager() {
        reelsAdapter = ReelsAdapter(
            onLikeClick = { post -> handleLikeClick(post) },
            onCommentClick = { post -> handleCommentClick(post) },
            onShareClick = { post -> handleShareClick(post) },
            onSaveClick = { post -> handleSaveClick(post) },
            onProfileClick = { post -> handleProfileClick(post) },
            onFollowClick = { post -> handleFollowClick(post) },
            onMoreClick = { post -> handleMoreClick(post) }
        )

        binding.reelsViewPager.apply {
            adapter = reelsAdapter
            orientation = ViewPager2.ORIENTATION_VERTICAL
            offscreenPageLimit = 1
            
            // Ensure user can scroll
            isUserInputEnabled = true
            
            // Disable clip for smooth scrolling feel
            (getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                clipToPadding = false
                clipChildren = false
            }

            // Auto-play on page change
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    
                    // Pause previous video
                    if (currentPosition != position) {
                        reelsAdapter.pauseAt(currentPosition)
                    }
                    
                    currentPosition = position
                    viewModel.setCurrentPosition(position)
                    
                    // Play new video
                    reelsAdapter.playAt(position)
                    
                    // Record view
                    val reels = viewModel.uiState.value.reels
                    if (position < reels.size) {
                        viewModel.recordVideoView(reels[position].postId)
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    // Pause during scroll to prevent audio overlap
                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                        reelsAdapter.pauseAt(currentPosition)
                    } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        reelsAdapter.playAt(currentPosition)
                    }
                }
            })
        }
    }

    private fun setupClickListeners() {
        binding.btnCamera.setOnClickListener {
            Toast.makeText(requireContext(), "Create Reel", Toast.LENGTH_SHORT).show()
            // TODO: Launch camera for creating reels
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

    private fun handleUiState(state: ReelsUiState) {
        // Loading state
        binding.progressLoading.isVisible = state.isLoading && state.reels.isEmpty()

        // Empty state
        binding.emptyState.isVisible = !state.isLoading && state.reels.isEmpty()

        // ViewPager visibility
        binding.reelsViewPager.isVisible = state.reels.isNotEmpty()

        // Submit reels to adapter
        reelsAdapter.submitList(state.reels)

        // Auto-play first reel when loaded
        if (state.reels.isNotEmpty() && currentPosition == 0) {
            binding.reelsViewPager.post {
                reelsAdapter.playAt(0)
            }
        }

        // Handle error
        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun handleLikeClick(post: Post) {
        if (post.isLikedByCurrentUser) {
            viewModel.unlikeReel(post.postId)
        } else {
            viewModel.likeReel(post.postId)
        }
    }

    private fun handleCommentClick(post: Post) {
        val intent = Intent(requireContext(), CommentsActivity::class.java)
        intent.putExtra("POST_ID", post.postId)
        startActivity(intent)
    }

    private fun handleShareClick(post: Post) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this reel by @${post.username}!")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun handleSaveClick(post: Post) {
        if (post.isSavedByCurrentUser) {
            viewModel.unsaveReel(post.postId)
            Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.saveReel(post.postId)
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleProfileClick(post: Post) {
        val intent = UserProfileActivity.newIntent(requireContext(), post.userId)
        startActivity(intent)
    }

    private fun handleFollowClick(post: Post) {
        Toast.makeText(requireContext(), "Follow @${post.username}", Toast.LENGTH_SHORT).show()
        // TODO: Implement follow functionality
    }

    private fun handleMoreClick(post: Post) {
        Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show()
        // TODO: Show bottom sheet with more options
    }

    override fun onResume() {
        super.onResume()
        // Set dark status bar for reels
        setDarkStatusBar()
        
        // Resume playback
        if (viewModel.uiState.value.reels.isNotEmpty()) {
            reelsAdapter.playAt(currentPosition)
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause all videos
        reelsAdapter.pauseAll()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            // Fragment is hidden (navigated away)
            reelsAdapter.pauseAll()
            resetStatusBar()
        } else {
            // Fragment is shown
            setDarkStatusBar()
            if (viewModel.uiState.value.reels.isNotEmpty()) {
                reelsAdapter.playAt(currentPosition)
            }
        }
    }

    private fun setDarkStatusBar() {
        activity?.window?.let { window ->
            window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            window.navigationBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = 0
            }
        }
    }

    private fun resetStatusBar() {
        activity?.window?.let { window ->
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.white)
            window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.white)
            
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reelsAdapter.releaseAll()
        resetStatusBar()
        _binding = null
    }
}
