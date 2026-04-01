package com.junaidjamshid.i211203.domain.usecase.highlight

import com.junaidjamshid.i211203.domain.model.StoryHighlight
import com.junaidjamshid.i211203.domain.repository.StoryHighlightRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting a user's story highlights.
 */
class GetUserHighlightsUseCase @Inject constructor(
    private val repository: StoryHighlightRepository
) {
    /**
     * Get highlights for a user as a one-time result.
     */
    suspend operator fun invoke(userId: String): Resource<List<StoryHighlight>> {
        return repository.getUserHighlights(userId)
    }
    
    /**
     * Get highlights for a user as a Flow for real-time updates.
     */
    fun asFlow(userId: String): Flow<Resource<List<StoryHighlight>>> {
        return repository.getUserHighlightsFlow(userId)
    }
}
