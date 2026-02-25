package com.junaidjamshid.i211203.domain.usecase.user

import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case to follow a user.
 */
class FollowUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(currentUserId: String, targetUserId: String): Resource<Unit> {
        if (currentUserId == targetUserId) {
            return Resource.Error("You cannot follow yourself")
        }
        return userRepository.followUser(currentUserId, targetUserId)
    }
}

/**
 * Use case to unfollow a user.
 */
class UnfollowUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(currentUserId: String, targetUserId: String): Resource<Unit> {
        return userRepository.unfollowUser(currentUserId, targetUserId)
    }
}
