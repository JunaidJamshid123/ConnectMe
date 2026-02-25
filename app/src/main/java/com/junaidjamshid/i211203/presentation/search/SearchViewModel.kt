package com.junaidjamshid.i211203.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Search functionality.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val currentUserId: String?
        get() = authRepository.getCurrentUserId()
    
    init {
        loadAllUsers()
        loadRecentSearches()
    }
    
    private fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            userRepository.getAllUsers().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val users = result.data?.filter { it.userId != currentUserId } ?: emptyList()
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                allUsers = users,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(isLoading = false, error = result.message) 
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
    
    private fun loadRecentSearches() {
        viewModelScope.launch {
            userRepository.getRecentSearches().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(recentSearches = result.data ?: emptyList())
                        }
                    }
                    else -> { /* Handle other states */ }
                }
            }
        }
    }
    
    /**
     * Search users by query
     */
    fun searchUsers(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        
        val lowerQuery = query.lowercase()
        val filtered = _uiState.value.allUsers.filter { user ->
            user.username.lowercase().contains(lowerQuery) ||
            user.fullName.lowercase().contains(lowerQuery)
        }
        
        _uiState.update { it.copy(searchResults = filtered) }
    }
    
    /**
     * Save user to recent searches
     */
    fun saveRecentSearch(user: User) {
        viewModelScope.launch {
            userRepository.saveRecentSearch(user.userId)
        }
    }
    
    /**
     * Remove user from recent searches
     */
    fun removeRecentSearch(user: User) {
        viewModelScope.launch {
            userRepository.removeRecentSearch(user.userId)
            _uiState.update { state ->
                state.copy(recentSearches = state.recentSearches.filter { it.userId != user.userId })
            }
        }
    }
    
    /**
     * Clear all recent searches
     */
    fun clearAllRecentSearches() {
        viewModelScope.launch {
            userRepository.clearAllRecentSearches()
            _uiState.update { it.copy(recentSearches = emptyList()) }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
