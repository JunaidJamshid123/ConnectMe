package com.junaidjamshid.i211203.domain.usecase.post

import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get feed posts for a user.
 */
class GetFeedPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(userId: String): Flow<Resource<List<Post>>> {
        return postRepository.getFeedPosts(userId)
    }
}
