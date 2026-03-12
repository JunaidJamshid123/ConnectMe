package com.junaidjamshid.i211203.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.domain.repository.MessageRepository
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var typingJob: Job? = null
    private var lastTypingTime: Long = 0
    
    val currentUserId: String?
        get() = authRepository.getCurrentUserId()
    
    /**
     * Initialize chat with another user
     */
    fun initializeChat(otherUserId: String) {
        receiverId = otherUserId
        loadOtherUserDetails(otherUserId)
        loadMessages(otherUserId)
        subscribeToOnlineStatus()
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
        
        // Subscribe to typing indicator after conversationId is set
        subscribeToTypingIndicator()
        
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
    
    /**
     * Called when user is typing
     * Throttles typing events to avoid too many updates
     */
    fun onUserTyping() {
        val currentTime = System.currentTimeMillis()
        
        // Only send typing event if more than 2 seconds since last one
        if (currentTime - lastTypingTime > 2000) {
            lastTypingTime = currentTime
            sendTypingIndicator(true)
        }
        
        // Cancel previous job
        typingJob?.cancel()
        
        // After 3 seconds of no typing, send stopped typing
        typingJob = viewModelScope.launch {
            delay(3000)
            sendTypingIndicator(false)
        }
    }
    
    /**
     * Called when user stops typing (e.g., loses focus)
     */
    fun onUserStoppedTyping() {
        typingJob?.cancel()
        sendTypingIndicator(false)
    }
    
    private fun sendTypingIndicator(isTyping: Boolean) {
        val conversationId = currentConversationId ?: return
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            messageRepository.sendTypingIndicator(conversationId, userId, isTyping)
        }
    }
    
    /**
     * Subscribe to typing indicators from other user
     */
    private fun subscribeToTypingIndicator() {
        val conversationId = currentConversationId ?: return
        val otherUserId = receiverId ?: return
        
        viewModelScope.launch {
            messageRepository.observeTypingIndicator(conversationId, otherUserId).collect { isTyping ->
                _uiState.update { it.copy(isOtherUserTyping = isTyping) }
            }
        }
    }
    
    /**
     * Subscribe to online status of other user
     */
    private fun subscribeToOnlineStatus() {
        val otherUserId = receiverId ?: return
        
        viewModelScope.launch {
            userRepository.observeUserOnlineStatus(otherUserId).collect { (isOnline, lastSeen) ->
                _uiState.update { 
                    it.copy(
                        isOtherUserOnline = isOnline,
                        lastSeenTimestamp = lastSeen
                    )
                }
            }
        }
    }
    
    /**
     * Format last seen timestamp to human-readable string
     */
    fun formatLastSeen(timestamp: Long?): String {
        if (timestamp == null) return ""
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Active now"
            diff < 3600_000 -> "Active ${diff / 60_000}m ago"
            diff < 86400_000 -> "Active ${diff / 3600_000}h ago"
            else -> {
                val days = diff / 86400_000
                if (days == 1L) "Active yesterday" else "Active ${days}d ago"
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Stop typing indicator when leaving chat
        onUserStoppedTyping()
    }
}
