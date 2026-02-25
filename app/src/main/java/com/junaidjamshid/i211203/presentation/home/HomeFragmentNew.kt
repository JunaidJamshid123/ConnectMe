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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.contacts.ContactsFragmentNew
import com.junaidjamshid.i211203.presentation.home.adapter.PostAdapterNew
import com.junaidjamshid.i211203.presentation.home.adapter.StoryAdapterNew
import com.junaidjamshid.i211203.presentation.main.MainActivityNew
import com.junaidjamshid.i211203.presentation.post.PostDetailActivity
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Home Fragment refactored to use Clean Architecture with ViewModel.
 */
@AndroidEntryPoint
class HomeFragmentNew : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapterNew
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapterNew
    
    private val TAG = "HomeFragmentNew"

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        setupViews(view)
        setupRecyclerViews(view)
        observeState()
        
        return view
    }
    
    private fun setupViews(view: View) {
        val addStory = view.findViewById<FrameLayout>(R.id.addStroy)
        val dms = view.findViewById<ImageView>(R.id.DMs)
        val currentUserImage = view.findViewById<ImageView>(R.id.current_user_image)
        
        dms.setOnClickListener {
            // Navigate to contacts/DMs tab
            (activity as? MainActivityNew)?.navigateToTab(R.id.nav_contacts)
        }
        
        addStory.setOnClickListener {
            // Navigate to add post tab
            (activity as? MainActivityNew)?.navigateToTab(R.id.nav_add_post)
        }
    }
    
    private fun setupRecyclerViews(view: View) {
        // Posts RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)
        postAdapter = PostAdapterNew(
            onLikeClick = { postId -> viewModel.onLikePost(postId) },
            onCommentClick = { postId -> onCommentClicked(postId) },
            onShareClick = { postId -> onShareClicked(postId) },
            onSaveClick = { postId -> onSaveClicked(postId) },
            onProfileClick = { userId -> onProfileClicked(userId) },
            onMenuClick = { post -> onMenuClicked(post) }
        )
        recyclerView.adapter = postAdapter
        
        // Stories RecyclerView
        storiesRecyclerView = view.findViewById(R.id.recycler_view_stories)
        storiesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        storyAdapter = StoryAdapterNew { story ->
            // Handle story click
            Log.d(TAG, "Story clicked: ${story.storyId}")
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
        
        // Update posts
        postAdapter.submitList(state.posts)
        
        // Update stories
        storyAdapter.submitList(state.stories)
        
        // Handle errors
        state.postsError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
        
        state.storiesError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
        
        // Show empty state if needed
        if (!state.isLoadingPosts && state.posts.isEmpty()) {
            // Handle empty state
            Log.d(TAG, "No posts found")
        }
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
        startActivity(PostDetailActivity.newIntent(requireContext(), postId))
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
        Toast.makeText(context, "Post saved", Toast.LENGTH_SHORT).show()
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
    
    companion object {
        @JvmStatic
        fun newInstance() = HomeFragmentNew()
    }
}
