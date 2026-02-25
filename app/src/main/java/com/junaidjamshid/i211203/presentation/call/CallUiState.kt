package com.junaidjamshid.i211203.presentation.call

import com.junaidjamshid.i211203.domain.model.Call

/**
 * UI State for Call screens.
 */
data class CallUiState(
    val isLoading: Boolean = false,
    val callHistory: List<Call> = emptyList(),
    val currentCall: Call? = null,
    val incomingCall: Call? = null,
    val isInCall: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isMicEnabled: Boolean = true,
    val isSpeakerEnabled: Boolean = true,
    val callDuration: Long = 0L,
    val error: String? = null
)
