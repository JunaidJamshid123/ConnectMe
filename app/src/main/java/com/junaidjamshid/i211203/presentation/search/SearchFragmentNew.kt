package com.junaidjamshid.i211203.presentation.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.databinding.FragmentSearchBinding
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import com.junaidjamshid.i211203.presentation.search.adapter.ExploreGridAdapter
import com.junaidjamshid.i211203.presentation.search.adapter.RecentSearchAdapterNew
import com.junaidjamshid.i211203.presentation.search.adapter.SearchAdapterNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style Search/Explore Fragment.
 * Default view: Explore grid with mixed tile sizes.
 * Search mode: Shows recent searches (empty query) or search results (with query).
 */
@AndroidEntryPoint
class SearchFragmentNew : Fragment() {
    
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()
    
    private lateinit var searchAdapter: SearchAdapterNew
    private lateinit var recentSearchAdapter: RecentSearchAdapterNew
    private lateinit var exploreAdapter: ExploreGridAdapter
    
    private var isSearchMode = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupExploreGrid()
        setupRecyclerViews()
        setupSearchListener()
        setupClickListeners()
        observeUiState()
        
        // Show explore grid by default
        showExploreMode()
    }
    
    /**
     * Setup Instagram-style explore grid with staggered tile sizes.
     * Uses GridLayoutManager with 3 columns and a SpanSizeLookup to make
     * certain tiles 2x2 (spanning 2 columns).
     */
    private fun setupExploreGrid() {
        exploreAdapter = ExploreGridAdapter(
            onPostClick = { post ->
                // TODO: Navigate to post detail
            }
        )
        
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (exploreAdapter.isLargeItem(position)) 2 else 1
            }
        }
        
        binding.rvExploreGrid.apply {
            layoutManager = gridLayoutManager
            adapter = exploreAdapter
            setHasFixedSize(false)
            
            // Add item decoration for 1dp gaps between grid items
            addItemDecoration(ExploreGridItemDecoration(1))
        }
    }
    
    private fun setupRecyclerViews() {
        // Search results adapter
        searchAdapter = SearchAdapterNew(
            onUserClick = { user -> onUserClick(user) },
            onRemoveClick = { /* Not needed for search results */ }
        )
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
        
        // Recent searches adapter
        recentSearchAdapter = RecentSearchAdapterNew(
            onUserClick = { user -> onUserClick(user) },
            onRemoveClick = { user -> viewModel.removeRecentSearch(user) }
        )
        binding.rvRecentSearches.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentSearchAdapter
        }
    }
    
    private fun setupSearchListener() {
        // Focus listener to enter/exit search mode
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                enterSearchMode()
            }
        }
        
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                viewModel.searchUsers(query)
                
                // Show/hide clear button
                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                
                if (query.isEmpty()) {
                    // Show recent searches, hide search results
                    binding.recentSearchesContainer.visibility = View.VISIBLE
                    binding.searchResultsContainer.visibility = View.GONE
                } else {
                    // Show search results, hide recent searches
                    binding.recentSearchesContainer.visibility = View.GONE
                    binding.searchResultsContainer.visibility = View.VISIBLE
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupClickListeners() {
        binding.tvClearAll.setOnClickListener {
            viewModel.clearAllRecentSearches()
        }
        
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.etSearch.clearFocus()
            hideKeyboard()
            showExploreMode()
        }
    }
    
    /**
     * Enter search mode: hide explore grid, show recent searches or results
     */
    private fun enterSearchMode() {
        if (isSearchMode) return
        isSearchMode = true
        
        binding.exploreContainer.visibility = View.GONE
        
        val query = binding.etSearch.text.toString().trim()
        if (query.isEmpty()) {
            binding.recentSearchesContainer.visibility = View.VISIBLE
            binding.searchResultsContainer.visibility = View.GONE
        } else {
            binding.recentSearchesContainer.visibility = View.GONE
            binding.searchResultsContainer.visibility = View.VISIBLE
        }
    }
    
    /**
     * Exit search mode: show explore grid, hide search overlays
     */
    private fun showExploreMode() {
        isSearchMode = false
        
        binding.exploreContainer.visibility = View.VISIBLE
        binding.recentSearchesContainer.visibility = View.GONE
        binding.searchResultsContainer.visibility = View.GONE
        binding.btnClearSearch.visibility = View.GONE
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
    
    private fun handleUiState(state: SearchUiState) {
        // Handle loading state with shimmer
        if (state.isLoadingExplore && state.explorePosts.isEmpty()) {
            binding.shimmerSearch.visibility = View.VISIBLE
            binding.shimmerSearch.startShimmer()
            binding.searchContent.visibility = View.GONE
            return
        } else {
            binding.shimmerSearch.stopShimmer()
            binding.shimmerSearch.visibility = View.GONE
            binding.searchContent.visibility = View.VISIBLE
        }
        
        // Update explore grid
        exploreAdapter.submitList(state.explorePosts)
        
        // Update search results
        searchAdapter.submitList(state.searchResults)
        
        // Update recent searches
        recentSearchAdapter.submitList(state.recentSearches)
        
        // Show/hide no searches message
        binding.tvNoSearches.visibility = if (state.recentSearches.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Show "Top accounts" header when there are results
        binding.tvSearchResults.visibility = if (state.searchResults.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun onUserClick(user: User) {
        viewModel.saveRecentSearch(user)
        
        val intent = Intent(requireContext(), UserProfileActivity::class.java)
        intent.putExtra("USER_ID", user.userId)
        startActivity(intent)
    }
    
    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onPause() {
        super.onPause()
        // Pause all videos when fragment is paused
        exploreAdapter.pauseAllVideos()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Release all video players
        exploreAdapter.releaseAllPlayers()
        _binding = null
    }
}
