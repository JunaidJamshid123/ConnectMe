package com.junaidjamshid.i211203.presentation.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.CallType
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.domain.repository.CallRepository
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
 * ViewModel for Call screens.
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    
    private var durationJob: Job? = null
    
    private val currentUserId: String?
        get() = authRepository.getCurrentUserId()
    
    init {
        loadCallHistory()
        listenForIncomingCalls()
    }
    
    /**
     * Load call history
     */
    fun loadCallHistory() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            
            val result = callRepository.getCallHistory(userId)
            when (result) {
                is Resource.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            callHistory = (result.data as? List<*>)?.filterIsInstance<com.junaidjamshid.i211203.domain.model.Call>() ?: emptyList(),
                            error = null
                        )
                    }
                }
                is Resource.Error<*> -> {
                    _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                is Resource.Loading<*> -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
    
    /**
     * Listen for incoming calls
     */
    private fun listenForIncomingCalls() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            callRepository.observeIncomingCalls(userId).collect { call ->
                _uiState.update { it.copy(incomingCall = call) }
            }
        }
    }
    
    /**
     * Initiate a call
     */
    fun initiateCall(
        receiverId: String,
        callType: CallType = CallType.VIDEO
    ) {
        viewModelScope.launch {
            val callerId = currentUserId ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            
            val result = callRepository.initiateCall(
                callerId = callerId,
                receiverId = receiverId,
                callType = callType
            )
            
            when (result) {
                is Resource.Success<*> -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            currentCall = result.data as? com.junaidjamshid.i211203.domain.model.Call,
                            isInCall = true,
                            error = null
                        )
                    }
                    startCallDurationTimer()
                }
                is Resource.Error<*> -> {
                    _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Accept incoming call
     */
    fun acceptCall() {
        val call = _uiState.value.incomingCall ?: return
        
        viewModelScope.launch {
            val result = callRepository.acceptCall(call.callId)
            
            when (result) {
                is Resource.Success<*> -> {
                    _uiState.update { state ->
                        state.copy(
                            currentCall = call,
                            incomingCall = null,
                            isInCall = true
                        )
                    }
                    startCallDurationTimer()
                }
                is Resource.Error<*> -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Reject incoming call
     */
    fun rejectCall() {
        val call = _uiState.value.incomingCall ?: return
        
        viewModelScope.launch {
            callRepository.rejectCall(call.callId)
            _uiState.update { it.copy(incomingCall = null) }
        }
    }
    
    /**
     * End current call
     */
    fun endCall() {
        val call = _uiState.value.currentCall ?: return
        
        viewModelScope.launch {
            callRepository.endCall(call.callId)
            stopCallDurationTimer()
            _uiState.update { state ->
                state.copy(
                    currentCall = null,
                    isInCall = false,
                    callDuration = 0L
                )
            }
        }
    }
    
    /**
     * Toggle video
     */
    fun toggleVideo() {
        _uiState.update { it.copy(isVideoEnabled = !it.isVideoEnabled) }
    }
    
    /**
     * Toggle microphone
     */
    fun toggleMic() {
        _uiState.update { it.copy(isMicEnabled = !it.isMicEnabled) }
    }
    
    /**
     * Toggle speaker
     */
    fun toggleSpeaker() {
        _uiState.update { it.copy(isSpeakerEnabled = !it.isSpeakerEnabled) }
    }
    
    private fun startCallDurationTimer() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(callDuration = it.callDuration + 1) }
            }
        }
    }
    
    private fun stopCallDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearIncomingCall() {
        _uiState.update { it.copy(incomingCall = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCallDurationTimer()
    }
}
