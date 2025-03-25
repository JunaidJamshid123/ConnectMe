package com.junaidjamshid.i211203

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val messageId: String = "",
    var isEdited: Boolean = false,
    var isDeleted: Boolean = false,
    var isVanishMode: Boolean = false,
    var isRead: Boolean = false,
    val editedTimestamp: Long = 0
)