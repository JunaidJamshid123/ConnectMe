package com.junaidjamshid.i211203.presentation.profile

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.FragmentProfileBinding
import com.junaidjamshid.i211203.presentation.auth.LoginActivity
import com.junaidjamshid.i211203.presentation.follow.FollowersActivity
import com.junaidjamshid.i211203.presentation.follow.FollowingActivity
import com.junaidjamshid.i211203.presentation.profile.adapter.PostGridAdapterNew
import com.junaidjamshid.i211203.presentation.profile.adapter.StoryHighlightAdapter
import com.junaidjamshid.i211203.presentation.story.AddStoryActivity
import com.junaidjamshid.i211203.presentation.story.StoryDisplayActivityNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style Profile Fragment with pixel-perfect design.
 */
@AndroidEntryPoint
class ProfileFragmentNew : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var postGridAdapter: PostGridAdapterNew
    private lateinit var highlightAdapter: StoryHighlightAdapter

    private var isGridTabActive = true
    private var hasShownContent = false

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
        setupHighlights()
        setupClickListeners()
        setupTabs()
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
            isNestedScrollingEnabled = false
        }
    }

    private fun setupHighlights() {
        highlightAdapter = StoryHighlightAdapter(
            onHighlightClick = { highlight ->
                Toast.makeText(requireContext(), "Opening: ${highlight.name}", Toast.LENGTH_SHORT).show()
            },
            onAddHighlightClick = {
                Toast.makeText(requireContext(), "Create new highlight", Toast.LENGTH_SHORT).show()
            }
        )

        binding.highlightsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = highlightAdapter
        }

        // No hardcoded highlights — only the "New" (+) button shows via the adapter
        // Real highlights would come from the ViewModel / backend when available
        highlightAdapter.submitList(emptyList())
    }

    private fun setupClickListeners() {
        // Profile image: tap to view own stories or add
        binding.profileImage.setOnClickListener {
            val userId = viewModel.uiState.value.user?.userId
            if (userId != null && viewModel.uiState.value.hasActiveStories) {
                startActivity(StoryDisplayActivityNew.newIntent(requireContext(), userId))
            } else {
                startActivity(Intent(requireContext(), AddStoryActivity::class.java))
            }
        }

        // Edit profile button
        binding.editProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // Share profile button
        binding.shareProfile.setOnClickListener {
            shareProfile()
        }

        // Discover people button
        binding.btnDiscoverPeople.setOnClickListener {
            Toast.makeText(requireContext(), "Discover People", Toast.LENGTH_SHORT).show()
        }

        // Add content (+) button
        binding.btnAddContent.setOnClickListener {
            Toast.makeText(requireContext(), "Create new post", Toast.LENGTH_SHORT).show()
        }

        // Hamburger menu - shows menu with logout, settings, etc.
        binding.btnMenu.setOnClickListener {
            showMenuPopup(it)
        }

        // Followers section
        binding.followers.setOnClickListener {
            val intent = Intent(requireContext(), FollowersActivity::class.java).apply {
                putExtra("USERNAME", viewModel.uiState.value.user?.username ?: "")
            }
            startActivity(intent)
        }

        // Following section
        binding.following.setOnClickListener {
            val intent = Intent(requireContext(), FollowingActivity::class.java).apply {
                putExtra("USERNAME", viewModel.uiState.value.user?.username ?: "")
            }
            startActivity(intent)
        }

        // Logout hidden view (backward compat)
        binding.logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Username dropdown arrow
        binding.dropdownArrow.setOnClickListener {
            Toast.makeText(requireContext(), "Switch accounts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        binding.tabGrid.setOnClickListener {
            if (!isGridTabActive) {
                isGridTabActive = true
                updateTabUI()
            }
        }

        binding.tabTagged.setOnClickListener {
            if (isGridTabActive) {
                isGridTabActive = false
                updateTabUI()
                // Show tagged posts or empty state
                Toast.makeText(requireContext(), "Tagged photos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTabUI() {
        if (isGridTabActive) {
            binding.tabGridIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.tabGridIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))
            binding.tabTaggedIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.instagram_text_gray))
            binding.tabTaggedIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.recyclerView.isVisible = true
        } else {
            binding.tabGridIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.instagram_text_gray))
            binding.tabGridIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.tabTaggedIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.tabTaggedIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))
            binding.recyclerView.isVisible = false
        }
    }

    private fun showMenuPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Settings")
        popup.menu.add(0, 2, 1, "Archive")
        popup.menu.add(0, 3, 2, "Your Activity")
        popup.menu.add(0, 4, 3, "QR Code")
        popup.menu.add(0, 5, 4, "Saved")
        popup.menu.add(0, 6, 5, "Close Friends")
        popup.menu.add(0, 7, 6, "Favorites")
        popup.menu.add(0, 8, 7, "Logout")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                8 -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> {
                    Toast.makeText(requireContext(), item.title, Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
        popup.show()
    }

    private fun shareProfile() {
        val username = binding.usernameText.text.toString()
        val shareText = "Check out $username's profile on ConnectMe!"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Profile"))
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
        // Shimmer: show while loading for the first time, hide once we have data
        if (state.isLoading && !hasShownContent) {
            binding.shimmerProfile.visibility = View.VISIBLE
            binding.shimmerProfile.startShimmer()
            binding.profileContent.visibility = View.GONE
        } else if (state.user != null) {
            if (!hasShownContent) {
                binding.shimmerProfile.stopShimmer()
                binding.shimmerProfile.visibility = View.GONE
                binding.profileContent.visibility = View.VISIBLE
                hasShownContent = true
            }
        }

        // Update user info
        state.user?.let { user ->
            binding.usernameText.text = user.username
            binding.fullNameText.text = user.fullName.ifEmpty { user.username }

            // Bio — show only when it has content
            val bioText = user.bio.trim()
            if (bioText.isNotEmpty()) {
                binding.bioText.visibility = View.VISIBLE
                binding.bioText.text = bioText
            } else {
                binding.bioText.visibility = View.GONE
            }

            // Category / Professional label (hidden — enable when backend supports it)
            binding.categoryText.visibility = View.GONE

            // Link / Website (hidden — enable when backend supports it)
            binding.linkText.visibility = View.GONE

            // Profile image
            if (!user.profilePicture.isNullOrEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(user.profilePicture, android.util.Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        binding.profileImage.setImageBitmap(bitmap)
                    } else {
                        binding.profileImage.setImageResource(R.drawable.default_profile)
                    }
                } catch (e: Exception) {
                    binding.profileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                binding.profileImage.setImageResource(R.drawable.default_profile)
            }
        }

        // Story ring
        binding.storyRing.isVisible = state.hasActiveStories

        // Counts
        binding.postsCount.text = formatCount(state.postsCount)
        binding.followersCount.text = formatCount(state.followersCount)
        binding.followingCount.text = formatCount(state.followingCount)

        // Highlights subtitle — show only when user has no highlights yet
        binding.highlightsSubtitle.isVisible = highlightAdapter.itemCount <= 1

        // Posts grid
        val hasPosts = state.posts.isNotEmpty()
        postGridAdapter.submitList(state.posts)
        binding.recyclerView.isVisible = hasPosts && isGridTabActive
        binding.emptyPostsContainer.isVisible = !hasPosts && !state.isLoading

        // Logout
        if (state.logoutSuccess) {
            navigateToLogin()
        }

        // Error
        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    /**
     * Format count Instagram-style: 1234 → "1,234", 12500 → "12.5K", 1200000 → "1.2M"
     */
    private fun formatCount(count: Int): String {
        return when {
            count < 10_000 -> String.format("%,d", count)
            count < 1_000_000 -> {
                val k = count / 1000.0
                if (k == k.toLong().toDouble()) "${k.toLong()}K"
                else String.format("%.1fK", k).replace(".0K", "K")
            }
            else -> {
                val m = count / 1_000_000.0
                if (m == m.toLong().toDouble()) "${m.toLong()}M"
                else String.format("%.1fM", m).replace(".0M", "M")
            }
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

    override fun onResume() {
        super.onResume()
        // Refresh profile when returning (e.g., after editing)
        viewModel.loadCurrentUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hasShownContent = false
        _binding = null
    }
}
