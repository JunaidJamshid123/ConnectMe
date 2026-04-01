package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.HighlightStory
import com.junaidjamshid.i211203.domain.model.StoryHighlight
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Story Highlight operations.
 */
interface StoryHighlightRepository {
    
    /**
     * Get all highlights for a user
     */
    suspend fun getUserHighlights(userId: String): Resource<List<StoryHighlight>>
    
    /**
     * Get highlights as a Flow for real-time updates
     */
    fun getUserHighlightsFlow(userId: String): Flow<Resource<List<StoryHighlight>>>
    
    /**
     * Get a single highlight by ID with all its stories
     */
    suspend fun getHighlight(highlightId: String): Resource<StoryHighlight>
    
    /**
     * Create a new highlight
     */
    suspend fun createHighlight(
        name: String,
        coverImageBytes: ByteArray? = null,
        storyImageUrls: List<String> = emptyList()
    ): Resource<StoryHighlight>
    
    /**
     * Add stories to an existing highlight
     */
    suspend fun addStoriesToHighlight(
        highlightId: String,
        storyImageUrls: List<String>
    ): Resource<List<HighlightStory>>
    
    /**
     * Update a highlight's name or cover image
     */
    suspend fun updateHighlight(
        highlightId: String,
        name: String? = null,
        coverImageBytes: ByteArray? = null
    ): Resource<Unit>
    
    /**
     * Delete a highlight and all its stories
     */
    suspend fun deleteHighlight(highlightId: String): Resource<Unit>
    
    /**
     * Remove a story from a highlight
     */
    suspend fun removeStoryFromHighlight(highlightId: String, storyId: Long): Resource<Unit>
}
