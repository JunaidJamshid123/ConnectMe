package com.junaidjamshid.i211203.data.dto

/**
 * Data Transfer Object for Story Highlight data.
 */
data class StoryHighlightDto(
    val highlightId: String = "",
    val userId: String = "",
    val name: String = "",
    val coverImageUrl: String = "",
    val stories: List<HighlightStoryDto> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val position: Int = 0
)

/**
 * Data Transfer Object for a story within a highlight.
 */
data class HighlightStoryDto(
    val id: Long = 0,
    val highlightId: String = "",
    val storyImageUrl: String = "",
    val timestamp: Long = 0,
    val position: Int = 0
)
