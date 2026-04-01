package com.junaidjamshid.i211203.domain.usecase.highlight

import com.junaidjamshid.i211203.domain.model.StoryHighlight
import com.junaidjamshid.i211203.domain.repository.StoryHighlightRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for getting a single highlight with all its stories.
 */
class GetHighlightUseCase @Inject constructor(
    private val repository: StoryHighlightRepository
) {
    /**
     * Get a highlight by ID.
     *
     * @param highlightId The ID of the highlight to fetch
     */
    suspend operator fun invoke(highlightId: String): Resource<StoryHighlight> {
        if (highlightId.isBlank()) {
            return Resource.Error("Highlight ID cannot be empty")
        }
        
        return repository.getHighlight(highlightId)
    }
}
