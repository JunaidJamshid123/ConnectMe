package com.junaidjamshid.i211203.util

object Constants {
    // Firebase Database References
    const val USERS_REF = "users"
    const val POSTS_REF = "posts"
    const val STORIES_REF = "stories"
    const val MESSAGES_REF = "messages"
    const val CONVERSATIONS_REF = "conversations"
    const val COMMENTS_REF = "comments"
    const val FOLLOWERS_REF = "followers"
    const val FOLLOWING_REF = "following"
    
    // Story expiration time (24 hours in milliseconds)
    const val STORY_EXPIRATION_TIME = 24 * 60 * 60 * 1000L
    
    // Agora App ID
    const val AGORA_APP_ID = "your_agora_app_id"
    
    // DataStore
    const val USER_PREFERENCES = "user_preferences"
    const val KEY_USER_ID = "user_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    // Pagination
    const val PAGE_SIZE = 20
}
