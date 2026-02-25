package com.junaidjamshid.i211203.domain.usecase.post

import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case to like a post.
 */
class LikePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Resource<Unit> {
        return postRepository.likePost(postId, userId)
    }
}
