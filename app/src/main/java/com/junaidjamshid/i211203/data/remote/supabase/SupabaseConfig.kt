package com.junaidjamshid.i211203.data.remote.supabase

/**
 * Supabase configuration constants.
 */
object SupabaseConfig {
    // Supabase URL
    const val SUPABASE_URL = "https://rjkptagdtrtbjerldkay.supabase.co"
    
    // Supabase anon/public key
    const val SUPABASE_ANON_KEY = "sb_publishable_pEP65wzo7BLpz80dKNFyfQ_wZAvnbu8"
    
    // Table names
    const val USERS_TABLE = "users"
    const val POSTS_TABLE = "posts"
    const val COMMENTS_TABLE = "comments"
    const val STORIES_TABLE = "stories"
    const val MESSAGES_TABLE = "messages"
    const val CONVERSATIONS_TABLE = "conversations"
    const val CALLS_TABLE = "calls"
    const val ACTIVE_CALLS_TABLE = "active_calls"
    const val FOLLOWERS_TABLE = "followers"
    const val LIKES_TABLE = "likes"
    const val STORY_VIEWERS_TABLE = "story_viewers"
    const val RECENT_SEARCHES_TABLE = "recent_searches"
    
    // Storage buckets
    const val PROFILE_IMAGES_BUCKET = "profile-images"
    const val POST_IMAGES_BUCKET = "post-images"
    const val STORY_IMAGES_BUCKET = "story-images"
    const val MESSAGE_IMAGES_BUCKET = "message-images"
}
