package com.junaidjamshid.i211203.presentation.story

import com.junaidjamshid.i211203.domain.model.Story

/**
 * UI State for Story screens.
 */
data class StoryUiState(
    val isLoading: Boolean = false,
    val stories: List<Story> = emptyList(),
    val currentStory: Story? = null,
    val currentStoryIndex: Int = 0,
    val storyProgress: Float = 0f,
    val storyCreated: Boolean = false,
    val error: String? = null
)
