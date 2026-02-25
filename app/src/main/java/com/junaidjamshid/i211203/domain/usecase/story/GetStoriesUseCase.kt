package com.junaidjamshid.i211203.domain.usecase.story

import com.junaidjamshid.i211203.domain.model.Story
import com.junaidjamshid.i211203.domain.repository.StoryRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get stories for feed.
 */
class GetStoriesUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    operator fun invoke(userId: String): Flow<Resource<List<Story>>> {
        return storyRepository.getStories(userId)
    }
}
