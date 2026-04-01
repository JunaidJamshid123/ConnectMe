package com.junaidjamshid.i211203.domain.usecase.highlight

import com.junaidjamshid.i211203.domain.repository.StoryHighlightRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for deleting a story highlight.
 */
class DeleteHighlightUseCase @Inject constructor(
    private val repository: StoryHighlightRepository
) {
    /**
     * Delete a highlight and all its stories.
     *
     * @param highlightId The ID of the highlight to delete
     */
    suspend operator fun invoke(highlightId: String): Resource<Unit> {
        if (highlightId.isBlank()) {
            return Resource.Error("Highlight ID cannot be empty")
        }
        
        return repository.deleteHighlight(highlightId)
    }
}
