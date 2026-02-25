package com.junaidjamshid.i211203.presentation.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.databinding.FragmentSearchBinding
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import com.junaidjamshid.i211203.presentation.search.adapter.RecentSearchAdapterNew
import com.junaidjamshid.i211203.presentation.search.adapter.SearchAdapterNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture Search Fragment.
 */
@AndroidEntryPoint
class SearchFragmentNew : Fragment() {
    
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()
    
    private lateinit var searchAdapter: SearchAdapterNew
    private lateinit var recentSearchAdapter: RecentSearchAdapterNew
    
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
        
        setupRecyclerViews()
        setupSearchListener()
        setupClickListeners()
        observeUiState()
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
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                viewModel.searchUsers(query)
                
                if (query.isEmpty()) {
                    binding.recentSearchesContainer.visibility = View.VISIBLE
                    binding.searchResultsContainer.visibility = View.GONE
                } else {
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
