package com.junaidjamshid.i211203.domain.model

/**
 * Enum representing the type of media in a post.
 */
enum class MediaType {
    /** Standard image post (single or carousel) */
    IMAGE,
    
    /** Video post in the feed (up to 60 seconds) */
    VIDEO,
    
    /** Short-form vertical video / Reel (up to 90 seconds) */
    REEL;
    
    companion object {
        fun fromString(value: String?): MediaType {
            return when (value?.lowercase()) {
                "video" -> VIDEO
                "reel" -> REEL
                else -> IMAGE
            }
        }
    }
    
    fun toDbValue(): String = name.lowercase()
}
