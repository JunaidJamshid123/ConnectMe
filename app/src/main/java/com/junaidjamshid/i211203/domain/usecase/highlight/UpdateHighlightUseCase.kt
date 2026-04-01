package com.junaidjamshid.i211203.domain.usecase.highlight

import com.junaidjamshid.i211203.domain.repository.StoryHighlightRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for updating a story highlight.
 */
class UpdateHighlightUseCase @Inject constructor(
    private val repository: StoryHighlightRepository
) {
    /**
     * Update a highlight's name or cover image.
     *
     * @param highlightId The ID of the highlight to update
     * @param name Optional new name for the highlight
     * @param coverImageBytes Optional new cover image as byte array
     */
    suspend operator fun invoke(
        highlightId: String,
        name: String? = null,
        coverImageBytes: ByteArray? = null
    ): Resource<Unit> {
        if (highlightId.isBlank()) {
            return Resource.Error("Highlight ID cannot be empty")
        }
        
        if (name != null && name.isBlank()) {
            return Resource.Error("Highlight name cannot be empty")
        }
        
        if (name != null && name.length > 30) {
            return Resource.Error("Highlight name is too long (max 30 characters)")
        }
        
        return repository.updateHighlight(highlightId, name, coverImageBytes)
    }
}
