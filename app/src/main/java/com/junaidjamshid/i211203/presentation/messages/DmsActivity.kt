package com.junaidjamshid.i211203.presentation.messages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.databinding.ActivityDmsNewBinding
import com.junaidjamshid.i211203.presentation.chat.ChatActivity
import com.junaidjamshid.i211203.presentation.messages.adapter.DmsFriendAdapter
import com.junaidjamshid.i211203.presentation.messages.adapter.DmsNoteAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style Direct Messages Activity.
 * Shows followed users (friends) for messaging.
 */
@AndroidEntryPoint
class DmsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDmsNewBinding
    private val viewModel: DmsViewModel by viewModels()

    private lateinit var friendsAdapter: DmsFriendAdapter
    private lateinit var notesAdapter: DmsNoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDmsNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set light status bar with dark icons
        window.statusBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setupViews()
        setupRecyclerViews()
        setupSearch()
        observeUiState()
    }

    private fun setupViews() {
        // Back button
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        // Notes/Stories horizontal list
        notesAdapter = DmsNoteAdapter { noteUser ->
            if (!noteUser.isCurrentUser) {
                openChat(noteUser.userId, noteUser.username, noteUser.profilePicture)
            }
        }
        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(this@DmsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = notesAdapter
            setHasFixedSize(false)
        }

        // Friends vertical list
        friendsAdapter = DmsFriendAdapter { friend ->
            openChat(friend.user.userId, friend.user.username, friend.user.profilePicture)
        }
        binding.rvFriends.apply {
            layoutManager = LinearLayoutManager(this@DmsActivity)
            adapter = friendsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchFriends(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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

    private fun handleUiState(state: DmsUiState) {
        // Update username in header
        state.currentUser?.let { user ->
            binding.tvUsername.text = user.username.ifEmpty { user.fullName }
        }

        // Loading
        binding.progressLoading.isVisible = state.isLoading && state.friends.isEmpty()

        // Empty state
        binding.emptyState.isVisible = !state.isLoading && state.friends.isEmpty()

        // Friends list - show filtered results (can be empty when no search matches)
        val displayFriends = if (binding.etSearch.text.isNullOrEmpty()) {
            state.friends
        } else {
            state.filteredFriends
        }
        binding.rvFriends.isVisible = state.friends.isNotEmpty()
        friendsAdapter.submitList(displayFriends)

        // Notes row - current user first, then online friends, then others
        val currentUserNote = state.currentUser?.let { user ->
            DmsNoteUser(
                userId = user.userId,
                username = "Your note",
                profilePicture = user.profilePicture,
                isOnline = false,
                isCurrentUser = true
            )
        } ?: DmsNoteUser("self", "Your note", null, false, isCurrentUser = true)

        val friendNotes = state.friends
            .sortedByDescending { it.isOnline }
            .take(15)
            .map { 
                DmsNoteUser(
                    it.user.userId, 
                    it.user.username.ifEmpty { it.user.fullName }, 
                    it.user.profilePicture, 
                    it.isOnline
                ) 
            }
        
        notesAdapter.submitList(listOf(currentUserNote) + friendNotes)

        // Error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun openChat(userId: String, username: String, profilePicture: String?) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("USER_ID", userId)
            // Note: Don't pass profile picture through Intent - it exceeds Binder transaction limit
            // ChatActivity fetches user data from repository using USER_ID
        }
        startActivity(intent)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, DmsActivity::class.java)
        }
    }
}
