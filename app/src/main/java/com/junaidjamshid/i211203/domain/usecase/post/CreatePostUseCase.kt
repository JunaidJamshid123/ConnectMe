package com.junaidjamshid.i211203.domain.usecase.post

import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case to create a new post.
 */
class CreatePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        userId: String,
        caption: String,
        imageBase64: String
    ): Resource<Post> {
        if (imageBase64.isBlank()) {
            return Resource.Error("Please select an image")
        }
        return postRepository.createPost(userId, caption, imageBase64)
    }
}
