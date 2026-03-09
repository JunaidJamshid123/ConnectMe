package com.junaidjamshid.i211203.presentation.discover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Discover People Activity - shows a list of suggested users to follow.
 * Instagram-style full page of suggestions.
 */
@AndroidEntryPoint
class DiscoverPeopleActivity : AppCompatActivity() {

    private val viewModel: DiscoverPeopleViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SuggestionListAdapter
    private var shimmerLayout: ShimmerFrameLayout? = null
    private var emptyText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover_people)

        setupViews()
        setupRecyclerView()
        observeState()
    }

    private fun setupViews() {
        val backButton = findViewById<ImageView>(R.id.btn_back)
        shimmerLayout = findViewById(R.id.shimmer_suggestions)
        emptyText = findViewById(R.id.empty_text)

        backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_suggestions)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SuggestionListAdapter(
            onFollowClick = { userId -> viewModel.onFollowClick(userId) },
            onDismissClick = { userId -> viewModel.onDismiss(userId) },
            onProfileClick = { userId ->
                startActivity(UserProfileActivity.newIntent(this, userId))
            }
        )
        recyclerView.adapter = adapter
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Shimmer
                    if (state.isLoading) {
                        shimmerLayout?.visibility = View.VISIBLE
                        shimmerLayout?.startShimmer()
                        recyclerView.visibility = View.GONE
                    } else {
                        shimmerLayout?.stopShimmer()
                        shimmerLayout?.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }

                    // Update following state
                    adapter.followingUserIds = state.followingUserIds

                    // Filter out dismissed users
                    val visible = state.suggestions.filter { it.userId !in state.dismissedUserIds }
                    adapter.submitList(visible)

                    // Empty state
                    if (!state.isLoading && visible.isEmpty()) {
                        emptyText?.visibility = View.VISIBLE
                        emptyText?.text = "No suggestions available"
                    } else {
                        emptyText?.visibility = View.GONE
                    }

                    // Error
                    state.error?.let {
                        Toast.makeText(this@DiscoverPeopleActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, DiscoverPeopleActivity::class.java)
        }
    }
}
