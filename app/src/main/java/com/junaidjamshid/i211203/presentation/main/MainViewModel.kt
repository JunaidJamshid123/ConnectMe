package com.junaidjamshid.i211203.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * ViewModel for Main Activity.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    val isLoggedIn: Boolean
        get() = authRepository.isUserLoggedIn()
    
    val currentUserId: String?
        get() = authRepository.getCurrentUserId()
    
    init {
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            userRepository.getUserByIdFlow(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(currentUser = result.data) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                    else -> { /* Loading */ }
                }
            }
        }
    }
    
    fun updateOnlineStatus(isOnline: Boolean) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            userRepository.updateOnlineStatus(userId, isOnline)
        }
    }
    
    fun updateLastSeen() {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            userRepository.updateLastSeen(userId)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            // Update online status before logout
            currentUserId?.let { userId ->
                userRepository.updateOnlineStatus(userId, false)
            }
            authRepository.logout()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
