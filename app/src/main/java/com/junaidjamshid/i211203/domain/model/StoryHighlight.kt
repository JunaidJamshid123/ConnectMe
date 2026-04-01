package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Story Highlight.
 * A highlight is a permanent collection of stories that appears on a user's profile.
 */
data class StoryHighlight(
    val highlightId: String = "",
    val userId: String = "",
    val name: String = "",
    val coverImageUrl: String = "",
    val stories: List<HighlightStory> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val position: Int = 0
)

/**
 * Domain model representing a single story within a highlight.
 */
data class HighlightStory(
    val id: Long = 0,
    val highlightId: String = "",
    val storyImageUrl: String = "",
    val timestamp: Long = 0,
    val position: Int = 0
)
