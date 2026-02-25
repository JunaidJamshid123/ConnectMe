package com.junaidjamshid.i211203.domain.usecase.story

import com.junaidjamshid.i211203.domain.model.Story
import com.junaidjamshid.i211203.domain.repository.StoryRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case to create a new story.
 */
class CreateStoryUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    suspend operator fun invoke(userId: String, imageBase64: String): Resource<Story> {
        if (imageBase64.isBlank()) {
            return Resource.Error("Please select an image")
        }
        return storyRepository.createStory(userId, imageBase64)
    }
}
