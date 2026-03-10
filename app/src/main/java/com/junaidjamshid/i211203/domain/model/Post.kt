package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Post in the application.
 * Supports Instagram-like features: multiple images (carousel), location, music,
 * and video/reel posts with auto-play support.
 */
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val caption: String = "",
    val location: String = "",
    val musicName: String = "",
    val musicArtist: String = "",
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val isSavedByCurrentUser: Boolean = false,
    // Video/Reel fields
    val mediaType: MediaType = MediaType.IMAGE,
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val videoDuration: Int = 0,       // Duration in milliseconds
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val aspectRatio: Float = 1f,      // height/width ratio for proper sizing
    val viewsCount: Int = 0           // Video view count
) {
    /** Whether this post is a carousel (multiple images) */
    val isCarousel: Boolean get() = mediaType == MediaType.IMAGE && imageUrls.size > 1
    
    /** Whether this post has music attached */
    val hasMusic: Boolean get() = musicName.isNotBlank()
    
    /** Whether this post has a location tagged */
    val hasLocation: Boolean get() = location.isNotBlank()
    
    /** Get all displayable images: imageUrls if present, else fallback to single postImageUrl */
    val allImages: List<String>
        get() = imageUrls.ifEmpty { if (postImageUrl.isNotEmpty()) listOf(postImageUrl) else emptyList() }
    
    /** Whether this post is a video or reel */
    val isVideo: Boolean get() = mediaType == MediaType.VIDEO || mediaType == MediaType.REEL
    
    /** Whether this post is specifically a reel (short-form vertical video) */
    val isReel: Boolean get() = mediaType == MediaType.REEL
    
    /** Get the display thumbnail - for videos use thumbnailUrl, for images use first image */
    val displayThumbnail: String
        get() = when {
            isVideo && thumbnailUrl.isNotBlank() -> thumbnailUrl
            isVideo && postImageUrl.isNotBlank() -> postImageUrl
            allImages.isNotEmpty() -> allImages.first()
            else -> ""
        }
    
    /** Format video duration for display (e.g., "0:15" or "1:30") */
    val formattedDuration: String
        get() {
            if (videoDuration <= 0) return ""
            val totalSeconds = videoDuration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}
