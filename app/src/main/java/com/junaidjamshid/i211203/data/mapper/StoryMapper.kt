package com.junaidjamshid.i211203.data.mapper

import com.junaidjamshid.i211203.data.dto.StoryDto
import com.junaidjamshid.i211203.domain.model.Story

/**
 * Mapper functions for Story data conversion.
 */
object StoryMapper {
    
    fun StoryDto.toDomain(currentUserId: String): Story {
        return Story(
            storyId = storyId,
            userId = userId,
            username = username,
            userProfileImage = userProfileImage,
            storyImageUrl = storyImageUrl,
            timestamp = timestamp,
            expiryTimestamp = expiryTimestamp,
            viewersCount = viewers.size,
            isViewedByCurrentUser = viewers.containsKey(currentUserId)
        )
    }
}
