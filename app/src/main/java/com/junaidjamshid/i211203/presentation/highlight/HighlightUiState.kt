package com.junaidjamshid.i211203.presentation.highlight

import android.net.Uri
import com.junaidjamshid.i211203.domain.model.HighlightStory
import com.junaidjamshid.i211203.domain.model.StoryHighlight

/**
 * UI State for Story Highlight screens.
 */
data class HighlightUiState(
    val isLoading: Boolean = false,
    val highlights: List<StoryHighlight> = emptyList(),
    val currentHighlight: StoryHighlight? = null,
    val currentStory: HighlightStory? = null,
    val currentStoryIndex: Int = 0,
    val storyProgress: Float = 0f,
    val highlightCreated: Boolean = false,
    val highlightDeleted: Boolean = false,
    val highlightUpdated: Boolean = false,
    val error: String? = null
)

/**
 * UI State for creating a new highlight.
 */
data class CreateHighlightUiState(
    val isLoading: Boolean = false,
    val highlightName: String = "",
    val selectedStoryUrls: List<String> = emptyList(),
    val selectedCoverUri: Uri? = null,
    val availableStories: List<SelectableStory> = emptyList(),
    val highlightCreated: Boolean = false,
    val error: String? = null
)

/**
 * Represents a selectable story in the create highlight flow.
 */
data class SelectableStory(
    val id: String,
    val imageUrl: String,
    val isSelected: Boolean = false
)
