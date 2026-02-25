package com.junaidjamshid.i211203.domain.usecase.user

import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get a user's profile.
 */
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Resource<User> {
        return userRepository.getUserById(userId)
    }
    
    fun observeUser(userId: String): Flow<Resource<User>> {
        return userRepository.getUserByIdFlow(userId)
    }
}
