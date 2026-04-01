package com.junaidjamshid.i211203.domain.usecase.highlight

import com.junaidjamshid.i211203.domain.model.StoryHighlight
import com.junaidjamshid.i211203.domain.repository.StoryHighlightRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for creating a new story highlight.
 */
class CreateHighlightUseCase @Inject constructor(
    private val repository: StoryHighlightRepository
) {
    /**
     * Create a new highlight with optional cover image and stories.
     *
     * @param name The name of the highlight
     * @param coverImageBytes Optional cover image as byte array
     * @param storyImageUrls List of story image URLs to include in the highlight
     */
    suspend operator fun invoke(
        name: String,
        coverImageBytes: ByteArray? = null,
        storyImageUrls: List<String> = emptyList()
    ): Resource<StoryHighlight> {
        if (name.isBlank()) {
            return Resource.Error("Highlight name cannot be empty")
        }
        
        if (name.length > 30) {
            return Resource.Error("Highlight name is too long (max 30 characters)")
        }
        
        return repository.createHighlight(name, coverImageBytes, storyImageUrls)
    }
}
