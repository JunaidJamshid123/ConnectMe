package com.junaidjamshid.i211203.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.domain.repository.MessageRepository
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
 * ViewModel for Contacts screen.
 */
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()
    
    init {
        loadContacts()
    }
    
    /**
     * Load contacts with recent conversations
     */
    fun loadContacts() {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            messageRepository.getConversations(currentUserId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val conversations = result.data ?: emptyList()
                        val contactItems = mutableListOf<ContactItem>()
                        
                        for (conversation in conversations) {
                            // Get the other user from the conversation
                            val otherUserId = conversation.participantIds
                                .firstOrNull { it != currentUserId } ?: continue
                            
                            // Fetch user details
                            val userResult = userRepository.getUserById(otherUserId)
                            
                            when (userResult) {
                                is Resource.Success -> {
                                    userResult.data?.let { user ->
                                        contactItems.add(
                                            ContactItem(
                                                user = user,
                                                lastMessage = conversation.lastMessage,
                                                lastMessageTime = conversation.lastMessageTimestamp,
                                                isOnline = user.isOnline
                                            )
                                        )
                                    }
                                }
                                else -> { /* Ignore errors for individual users */ }
                            }
                        }
                        
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                contacts = contactItems.sortedByDescending { it.lastMessageTime },
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
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
