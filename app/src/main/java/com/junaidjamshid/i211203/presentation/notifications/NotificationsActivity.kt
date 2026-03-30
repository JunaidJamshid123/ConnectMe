package com.junaidjamshid.i211203.presentation.notifications

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityNotificationsBinding
import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.presentation.post.PostDetailActivity
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style Notifications Activity.
 * Shows likes, comments, follows grouped by time period.
 */
@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var notificationsAdapter: NotificationsAdapter

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemUI()
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeUiState()
    }

    private fun setupSystemUI() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)

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

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                handleNotificationClick(notification)
            },
            onProfileClick = { notification ->
                navigateToProfile(notification.userId)
            },
            onFollowClick = { notification ->
                viewModel.onFollowClick(notification.userId)
            },
            onPostClick = { notification ->
                notification.postId?.let { postId ->
                    navigateToPost(postId)
                }
            }
        )

        binding.recyclerNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationsAdapter
            setHasFixedSize(false)
            itemAnimator = null
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            R.color.instagram_blue,
            R.color.instagram_pink,
            R.color.instagram_orange
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onRefresh()
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

    private fun handleUiState(state: NotificationsUiState) {
        // Loading state
        binding.shimmerNotifications.isVisible = state.isLoading && state.notifications.isEmpty()
        if (state.isLoading && state.notifications.isEmpty()) {
            binding.shimmerNotifications.startShimmer()
        } else {
            binding.shimmerNotifications.stopShimmer()
        }

        // Refresh state
        binding.swipeRefresh.isRefreshing = state.isRefreshing

        // Empty state
        binding.emptyState.isVisible = !state.isLoading && state.notifications.isEmpty()

        // Content
        binding.recyclerNotifications.isVisible = state.notifications.isNotEmpty()
        notificationsAdapter.submitGroupedList(state.groupedNotifications)
    }

    private fun handleNotificationClick(notification: Notification) {
        // Mark as read
        viewModel.markAsRead(notification.notificationId)

        // Navigate based on type
        when {
            notification.postId != null -> navigateToPost(notification.postId)
            else -> navigateToProfile(notification.userId)
        }
    }

    private fun navigateToProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
    }

    private fun navigateToPost(postId: String) {
        val intent = Intent(this, PostDetailActivity::class.java).apply {
            putExtra("POST_ID", postId)
        }
        startActivity(intent)
    }
}
