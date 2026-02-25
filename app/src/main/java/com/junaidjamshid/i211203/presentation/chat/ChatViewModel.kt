package com.junaidjamshid.i211203.presentation.chat

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
 * ViewModel for Chat screen.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var currentConversationId: String? = null
    private var receiverId: String? = null
    
    val currentUserId: String?
        get() = authRepository.getCurrentUserId()
    
    /**
     * Initialize chat with another user
     */
    fun initializeChat(otherUserId: String) {
        receiverId = otherUserId
        loadOtherUserDetails(otherUserId)
        loadMessages(otherUserId)
    }
    
    private fun loadOtherUserDetails(userId: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(otherUser = result.data) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                    else -> { /* Loading state */ }
                }
            }
        }
    }
    
    private fun loadMessages(otherUserId: String) {
        val currentId = currentUserId ?: return
        val conversationId = getConversationId(currentId, otherUserId)
        currentConversationId = conversationId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            messageRepository.getMessages(conversationId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                messages = result.data ?: emptyList(),
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
    
    /**
     * Send a message
     */
    fun sendMessage(messageText: String) {
        val receiver = receiverId ?: return
        val currentId = currentUserId ?: return
        val conversationId = currentConversationId ?: getConversationId(currentId, receiver)
        
        viewModelScope.launch {
            val message = com.junaidjamshid.i211203.domain.model.Message(
                messageId = "",
                conversationId = conversationId,
                senderId = currentId,
                receiverId = receiver,
                content = messageText,
                timestamp = System.currentTimeMillis(),
                messageType = if (_uiState.value.isVanishModeEnabled) 
                    com.junaidjamshid.i211203.domain.model.MessageType.TEXT 
                else 
                    com.junaidjamshid.i211203.domain.model.MessageType.TEXT
            )
            
            val result = messageRepository.sendMessage(message)
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(messageSent = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Toggle vanish mode
     */
    fun toggleVanishMode() {
        _uiState.update { it.copy(isVanishModeEnabled = !it.isVanishModeEnabled) }
    }
    
    /**
     * Mark message as read
     */
    fun markMessageAsRead(messageId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            messageRepository.markMessageAsRead(messageId, conversationId)
        }
    }
    
    /**
     * Delete a message
     */
    fun deleteMessage(messageId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId, conversationId)
        }
    }
    
    private fun getConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetMessageSent() {
        _uiState.update { it.copy(messageSent = false) }
    }
}
