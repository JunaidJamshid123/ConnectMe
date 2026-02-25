package com.junaidjamshid.i211203.domain.usecase.user

import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case to search for users.
 */
class SearchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(query: String): Resource<List<User>> {
        if (query.isBlank()) {
            return Resource.Success(emptyList())
        }
        return userRepository.searchUsers(query)
    }
}
