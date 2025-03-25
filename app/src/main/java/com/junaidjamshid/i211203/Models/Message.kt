package com.junaidjamshid.i211203.Models

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val messageId: String = ""
)
