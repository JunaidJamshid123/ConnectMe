package com.junaidjamshid.i211203.domain.usecase.post

import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for unliking a post.
 */
class UnlikePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Resource<Unit> {
        return postRepository.unlikePost(postId, userId)
    }
    
    suspend operator fun invoke(postId: String): Resource<Unit> {
        return postRepository.unlikePost(postId)
    }
}
