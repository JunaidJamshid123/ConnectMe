package com.junaidjamshid.i211203.presentation.main

import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Main Activity.
 */
data class MainUiState(
    val currentUser: User? = null,
    val unreadNotifications: Int = 0,
    val unreadMessages: Int = 0,
    val error: String? = null
)
