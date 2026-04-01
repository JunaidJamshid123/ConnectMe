package com.junaidjamshid.i211203.domain.usecase.highlight

import com.junaidjamshid.i211203.domain.model.HighlightStory
import com.junaidjamshid.i211203.domain.repository.StoryHighlightRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for adding stories to an existing highlight.
 */
class AddStoriesToHighlightUseCase @Inject constructor(
    private val repository: StoryHighlightRepository
) {
    /**
     * Add stories to an existing highlight.
     *
     * @param highlightId The ID of the highlight
     * @param storyImageUrls List of story image URLs to add
     */
    suspend operator fun invoke(
        highlightId: String,
        storyImageUrls: List<String>
    ): Resource<List<HighlightStory>> {
        if (highlightId.isBlank()) {
            return Resource.Error("Highlight ID cannot be empty")
        }
        
        if (storyImageUrls.isEmpty()) {
            return Resource.Error("No stories to add")
        }
        
        return repository.addStoriesToHighlight(highlightId, storyImageUrls)
    }
}
