package com.junaidjamshid.i211203.data.mapper

import com.junaidjamshid.i211203.data.dto.HighlightStoryDto
import com.junaidjamshid.i211203.data.dto.StoryHighlightDto
import com.junaidjamshid.i211203.domain.model.HighlightStory
import com.junaidjamshid.i211203.domain.model.StoryHighlight

/**
 * Mapper for converting between StoryHighlight domain and DTO objects.
 */
object StoryHighlightMapper {
    
    /**
     * Convert DTO to domain model
     */
    fun StoryHighlightDto.toDomain(): StoryHighlight = StoryHighlight(
        highlightId = highlightId,
        userId = userId,
        name = name,
        coverImageUrl = coverImageUrl,
        stories = stories.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        position = position
    )
    
    /**
     * Convert domain model to DTO
     */
    fun StoryHighlight.toDto(): StoryHighlightDto = StoryHighlightDto(
        highlightId = highlightId,
        userId = userId,
        name = name,
        coverImageUrl = coverImageUrl,
        stories = stories.map { it.toDto() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        position = position
    )
    
    /**
     * Convert HighlightStory DTO to domain model
     */
    fun HighlightStoryDto.toDomain(): HighlightStory = HighlightStory(
        id = id,
        highlightId = highlightId,
        storyImageUrl = storyImageUrl,
        timestamp = timestamp,
        position = position
    )
    
    /**
     * Convert HighlightStory domain model to DTO
     */
    fun HighlightStory.toDto(): HighlightStoryDto = HighlightStoryDto(
        id = id,
        highlightId = highlightId,
        storyImageUrl = storyImageUrl,
        timestamp = timestamp,
        position = position
    )
}
