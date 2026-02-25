package com.junaidjamshid.i211203.di

import com.junaidjamshid.i211203.data.repository.AuthRepositoryImpl
import com.junaidjamshid.i211203.data.repository.CallRepositoryImpl
import com.junaidjamshid.i211203.data.repository.MessageRepositoryImpl
import com.junaidjamshid.i211203.data.repository.PostRepositoryImpl
import com.junaidjamshid.i211203.data.repository.StoryRepositoryImpl
import com.junaidjamshid.i211203.data.repository.UserRepositoryImpl
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.domain.repository.CallRepository
import com.junaidjamshid.i211203.domain.repository.MessageRepository
import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.domain.repository.StoryRepository
import com.junaidjamshid.i211203.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository
    
    @Binds
    @Singleton
    abstract fun bindStoryRepository(
        storyRepositoryImpl: StoryRepositoryImpl
    ): StoryRepository
    
    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository
    
    @Binds
    @Singleton
    abstract fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository
}
