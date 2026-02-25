package com.junaidjamshid.i211203.data.mapper

import com.junaidjamshid.i211203.data.dto.UserDto
import com.junaidjamshid.i211203.domain.model.User

/**
 * Mapper functions for User data conversion.
 */
object UserMapper {
    
    fun UserDto.toDomain(): User {
        return User(
            userId = userId,
            username = username,
            email = email,
            fullName = fullName,
            phoneNumber = phoneNumber,
            profilePicture = profilePicture,
            coverPhoto = coverPhoto,
            bio = bio,
            followersCount = followers.size,
            followingCount = following.size,
            postsCount = 0, // Will be calculated separately
            isOnline = onlineStatus,
            pushToken = pushToken,
            createdAt = createdAt,
            lastSeen = lastSeen,
            vanishModeEnabled = vanishModeEnabled
        )
    }
    
    fun User.toDto(): UserDto {
        return UserDto(
            userId = userId,
            username = username,
            email = email,
            fullName = fullName,
            phoneNumber = phoneNumber,
            profilePicture = profilePicture,
            coverPhoto = coverPhoto,
            bio = bio,
            onlineStatus = isOnline,
            pushToken = pushToken,
            createdAt = createdAt,
            lastSeen = lastSeen,
            vanishModeEnabled = vanishModeEnabled
        )
    }
    
    fun User.toUpdateMap(): Map<String, Any> {
        return mapOf(
            "username" to username,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "bio" to bio
        ).filterValues { it.isNotEmpty() }
    }
}
