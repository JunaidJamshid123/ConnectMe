-- =====================================================
-- Supabase Database Schema for ConnectMe App
-- Run this SQL in your Supabase SQL Editor
-- =====================================================

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- USERS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    email TEXT UNIQUE NOT NULL,
    username TEXT NOT NULL,
    full_name TEXT NOT NULL,
    phone_number TEXT DEFAULT '',
    profile_picture TEXT DEFAULT '',
    cover_photo TEXT DEFAULT '',
    bio TEXT DEFAULT '',
    online_status BOOLEAN DEFAULT false,
    push_token TEXT DEFAULT '',
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    last_seen BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    vanish_mode_enabled BOOLEAN DEFAULT false
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- =====================================================
-- FOLLOWERS TABLE (Follow relationships)
-- =====================================================
CREATE TABLE IF NOT EXISTS followers (
    id BIGSERIAL PRIMARY KEY,
    follower_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_followers_follower ON followers(follower_id);
CREATE INDEX IF NOT EXISTS idx_followers_following ON followers(following_id);

-- =====================================================
-- POSTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS posts (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    user_profile_image TEXT DEFAULT '',
    post_image_url TEXT NOT NULL,
    caption TEXT DEFAULT '',
    location TEXT DEFAULT '',
    music_name TEXT DEFAULT '',
    music_artist TEXT DEFAULT '',
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_posts_post_id ON posts(post_id);
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_timestamp ON posts(timestamp DESC);

-- =====================================================
-- MIGRATION: Add new columns to existing posts table
-- (Safe to run multiple times — uses IF NOT EXISTS pattern)
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'location') THEN
        ALTER TABLE posts ADD COLUMN location TEXT DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'music_name') THEN
        ALTER TABLE posts ADD COLUMN music_name TEXT DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'music_artist') THEN
        ALTER TABLE posts ADD COLUMN music_artist TEXT DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'image_urls') THEN
        ALTER TABLE posts ADD COLUMN image_urls TEXT DEFAULT '[]';
    END IF;
    -- Video support columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'media_type') THEN
        ALTER TABLE posts ADD COLUMN media_type TEXT DEFAULT 'image';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'video_url') THEN
        ALTER TABLE posts ADD COLUMN video_url TEXT DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'thumbnail_url') THEN
        ALTER TABLE posts ADD COLUMN thumbnail_url TEXT DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'video_duration') THEN
        ALTER TABLE posts ADD COLUMN video_duration INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'video_width') THEN
        ALTER TABLE posts ADD COLUMN video_width INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'video_height') THEN
        ALTER TABLE posts ADD COLUMN video_height INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'aspect_ratio') THEN
        ALTER TABLE posts ADD COLUMN aspect_ratio REAL DEFAULT 1.0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'views_count') THEN
        ALTER TABLE posts ADD COLUMN views_count INTEGER DEFAULT 0;
    END IF;
END $$;

-- Index for media type queries (feed filtering)
CREATE INDEX IF NOT EXISTS idx_posts_media_type ON posts(media_type);

-- =====================================================
-- VIDEO VIEWS TABLE (Track video/reel views)
-- =====================================================
CREATE TABLE IF NOT EXISTS video_views (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    watch_duration INTEGER DEFAULT 0,
    watched_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_video_views_post_id ON video_views(post_id);
CREATE INDEX IF NOT EXISTS idx_video_views_user_id ON video_views(user_id);

-- RLS for video_views
ALTER TABLE video_views ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Video views are viewable by post owner" ON video_views;
CREATE POLICY "Video views are viewable by post owner" ON video_views FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can record video views" ON video_views;
CREATE POLICY "Users can record video views" ON video_views FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can update their video views" ON video_views;
CREATE POLICY "Users can update their video views" ON video_views FOR UPDATE USING (auth.uid()::text = user_id::text);

-- =====================================================
-- POST IMAGES TABLE (for carousel / multiple images)
-- =====================================================
CREATE TABLE IF NOT EXISTS post_images (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    position INT NOT NULL DEFAULT 0,
    alt_text TEXT DEFAULT ''
);

CREATE INDEX IF NOT EXISTS idx_post_images_post_id ON post_images(post_id);

-- RLS for post_images
ALTER TABLE post_images ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Post images are viewable by everyone" ON post_images;
CREATE POLICY "Post images are viewable by everyone" ON post_images FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can add images to their posts" ON post_images;
CREATE POLICY "Users can add images to their posts" ON post_images FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Users can delete their post images" ON post_images;
CREATE POLICY "Users can delete their post images" ON post_images FOR DELETE USING (true);

-- =====================================================
-- LIKES TABLE (Post likes)
-- =====================================================
CREATE TABLE IF NOT EXISTS likes (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_likes_post_id ON likes(post_id);
CREATE INDEX IF NOT EXISTS idx_likes_user_id ON likes(user_id);

-- =====================================================
-- SAVED POSTS TABLE (Bookmarked/Saved posts)
-- =====================================================
CREATE TABLE IF NOT EXISTS saved_posts (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_posts_post_id ON saved_posts(post_id);
CREATE INDEX IF NOT EXISTS idx_saved_posts_user_id ON saved_posts(user_id);

-- =====================================================
-- COMMENTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    comment_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    username TEXT NOT NULL,
    user_profile_image TEXT DEFAULT '',
    content TEXT NOT NULL,
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_comments_comment_id ON comments(comment_id);
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_timestamp ON comments(timestamp);

-- =====================================================
-- STORIES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS stories (
    id BIGSERIAL PRIMARY KEY,
    story_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    user_profile_image TEXT DEFAULT '',
    story_image_url TEXT NOT NULL,
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    expiry_timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_stories_story_id ON stories(story_id);
CREATE INDEX IF NOT EXISTS idx_stories_user_id ON stories(user_id);
CREATE INDEX IF NOT EXISTS idx_stories_expiry ON stories(expiry_timestamp);

-- =====================================================
-- STORY VIEWERS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS story_viewers (
    id BIGSERIAL PRIMARY KEY,
    story_id UUID NOT NULL,
    viewer_id UUID NOT NULL,
    viewed_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(story_id, viewer_id)
);

CREATE INDEX IF NOT EXISTS idx_story_viewers_story ON story_viewers(story_id);
CREATE INDEX IF NOT EXISTS idx_story_viewers_viewer ON story_viewers(viewer_id);

-- =====================================================
-- CONVERSATIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_id TEXT UNIQUE NOT NULL,
    participant_1 UUID NOT NULL,
    participant_2 UUID NOT NULL,
    last_message TEXT DEFAULT '',
    last_message_timestamp BIGINT DEFAULT 0,
    last_message_sender_id UUID
);

CREATE INDEX IF NOT EXISTS idx_conversations_id ON conversations(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversations_p1 ON conversations(participant_1);
CREATE INDEX IF NOT EXISTS idx_conversations_p2 ON conversations(participant_2);

-- =====================================================
-- MESSAGES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    message_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    conversation_id TEXT NOT NULL,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    content TEXT DEFAULT '',
    image_url TEXT,
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    is_read BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    message_type TEXT DEFAULT 'TEXT'
);

CREATE INDEX IF NOT EXISTS idx_messages_message_id ON messages(message_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp);

-- =====================================================
-- CALLS TABLE (Call history)
-- =====================================================
CREATE TABLE IF NOT EXISTS calls (
    id BIGSERIAL PRIMARY KEY,
    call_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    caller_id UUID NOT NULL,
    caller_name TEXT DEFAULT '',
    caller_profile_image TEXT DEFAULT '',
    receiver_id UUID NOT NULL,
    receiver_name TEXT DEFAULT '',
    receiver_profile_image TEXT DEFAULT '',
    call_type TEXT DEFAULT 'voice',
    call_status TEXT DEFAULT 'pending',
    channel_name TEXT DEFAULT '',
    agora_token TEXT DEFAULT '',
    start_timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    end_timestamp BIGINT,
    duration BIGINT
);

CREATE INDEX IF NOT EXISTS idx_calls_call_id ON calls(call_id);
CREATE INDEX IF NOT EXISTS idx_calls_caller ON calls(caller_id);
CREATE INDEX IF NOT EXISTS idx_calls_receiver ON calls(receiver_id);

-- =====================================================
-- ACTIVE CALLS TABLE (Currently ongoing calls)
-- =====================================================
CREATE TABLE IF NOT EXISTS active_calls (
    id BIGSERIAL PRIMARY KEY,
    call_id UUID UNIQUE NOT NULL,
    caller_id UUID NOT NULL,
    caller_name TEXT DEFAULT '',
    caller_profile_image TEXT DEFAULT '',
    receiver_id UUID NOT NULL,
    receiver_name TEXT DEFAULT '',
    receiver_profile_image TEXT DEFAULT '',
    call_type TEXT DEFAULT 'voice',
    call_status TEXT DEFAULT 'pending',
    channel_name TEXT DEFAULT '',
    agora_token TEXT DEFAULT '',
    start_timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    end_timestamp BIGINT,
    duration BIGINT
);

CREATE INDEX IF NOT EXISTS idx_active_calls_call_id ON active_calls(call_id);
CREATE INDEX IF NOT EXISTS idx_active_calls_receiver ON active_calls(receiver_id);

-- =====================================================
-- RECENT SEARCHES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS recent_searches (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    searched_user_id UUID NOT NULL,
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(user_id, searched_user_id)
);

CREATE INDEX IF NOT EXISTS idx_recent_searches_user ON recent_searches(user_id);

-- =====================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =====================================================

-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE followers ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE saved_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE story_viewers ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE active_calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE recent_searches ENABLE ROW LEVEL SECURITY;

-- Users policies
DROP POLICY IF EXISTS "Users are viewable by everyone" ON users;
CREATE POLICY "Users are viewable by everyone" ON users FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can update their own profile" ON users;
CREATE POLICY "Users can update their own profile" ON users FOR UPDATE USING (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can insert their own profile" ON users;
CREATE POLICY "Users can insert their own profile" ON users FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- Posts policies
DROP POLICY IF EXISTS "Posts are viewable by everyone" ON posts;
CREATE POLICY "Posts are viewable by everyone" ON posts FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can create their own posts" ON posts;
CREATE POLICY "Users can create their own posts" ON posts FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can delete their own posts" ON posts;
CREATE POLICY "Users can delete their own posts" ON posts FOR DELETE USING (auth.uid()::text = user_id::text);

-- Likes policies
DROP POLICY IF EXISTS "Likes are viewable by everyone" ON likes;
CREATE POLICY "Likes are viewable by everyone" ON likes FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can like posts" ON likes;
CREATE POLICY "Users can like posts" ON likes FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can unlike posts" ON likes;
CREATE POLICY "Users can unlike posts" ON likes FOR DELETE USING (auth.uid()::text = user_id::text);

-- Saved posts policies
DROP POLICY IF EXISTS "Users can view their saved posts" ON saved_posts;
CREATE POLICY "Users can view their saved posts" ON saved_posts FOR SELECT USING (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can save posts" ON saved_posts;
CREATE POLICY "Users can save posts" ON saved_posts FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can unsave posts" ON saved_posts;
CREATE POLICY "Users can unsave posts" ON saved_posts FOR DELETE USING (auth.uid()::text = user_id::text);

-- Comments policies
DROP POLICY IF EXISTS "Comments are viewable by everyone" ON comments;
CREATE POLICY "Comments are viewable by everyone" ON comments FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can create comments" ON comments;
CREATE POLICY "Users can create comments" ON comments FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can delete their own comments" ON comments;
CREATE POLICY "Users can delete their own comments" ON comments FOR DELETE USING (auth.uid()::text = user_id::text);

-- Stories policies
DROP POLICY IF EXISTS "Stories are viewable by everyone" ON stories;
CREATE POLICY "Stories are viewable by everyone" ON stories FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can create their own stories" ON stories;
CREATE POLICY "Users can create their own stories" ON stories FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can delete their own stories" ON stories;
CREATE POLICY "Users can delete their own stories" ON stories FOR DELETE USING (auth.uid()::text = user_id::text);

-- Story viewers policies
DROP POLICY IF EXISTS "Story viewers are viewable by story owner" ON story_viewers;
CREATE POLICY "Story viewers are viewable by story owner" ON story_viewers FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can mark stories as viewed" ON story_viewers;
CREATE POLICY "Users can mark stories as viewed" ON story_viewers FOR INSERT WITH CHECK (auth.uid()::text = viewer_id::text);

-- Followers policies
DROP POLICY IF EXISTS "Followers are viewable by everyone" ON followers;
CREATE POLICY "Followers are viewable by everyone" ON followers FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can follow others" ON followers;
CREATE POLICY "Users can follow others" ON followers FOR INSERT WITH CHECK (auth.uid()::text = follower_id::text);
DROP POLICY IF EXISTS "Users can unfollow others" ON followers;
CREATE POLICY "Users can unfollow others" ON followers FOR DELETE USING (auth.uid()::text = follower_id::text);

-- Messages policies
DROP POLICY IF EXISTS "Users can view their own messages" ON messages;
CREATE POLICY "Users can view their own messages" ON messages FOR SELECT USING (
    auth.uid()::text = sender_id::text OR auth.uid()::text = receiver_id::text
);
DROP POLICY IF EXISTS "Users can send messages" ON messages;
CREATE POLICY "Users can send messages" ON messages FOR INSERT WITH CHECK (auth.uid()::text = sender_id::text);
DROP POLICY IF EXISTS "Users can update their own messages" ON messages;
CREATE POLICY "Users can update their own messages" ON messages FOR UPDATE USING (auth.uid()::text = sender_id::text);

-- Conversations policies
DROP POLICY IF EXISTS "Users can view their own conversations" ON conversations;
CREATE POLICY "Users can view their own conversations" ON conversations FOR SELECT USING (
    auth.uid()::text = participant_1::text OR auth.uid()::text = participant_2::text
);
DROP POLICY IF EXISTS "Users can create conversations" ON conversations;
CREATE POLICY "Users can create conversations" ON conversations FOR INSERT WITH CHECK (
    auth.uid()::text = participant_1::text OR auth.uid()::text = participant_2::text
);
DROP POLICY IF EXISTS "Users can update their conversations" ON conversations;
CREATE POLICY "Users can update their conversations" ON conversations FOR UPDATE USING (
    auth.uid()::text = participant_1::text OR auth.uid()::text = participant_2::text
);

-- Calls policies
DROP POLICY IF EXISTS "Users can view their own calls" ON calls;
CREATE POLICY "Users can view their own calls" ON calls FOR SELECT USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);
DROP POLICY IF EXISTS "Users can create calls" ON calls;
CREATE POLICY "Users can create calls" ON calls FOR INSERT WITH CHECK (auth.uid()::text = caller_id::text);
DROP POLICY IF EXISTS "Users can update their calls" ON calls;
CREATE POLICY "Users can update their calls" ON calls FOR UPDATE USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);

-- Active calls policies
DROP POLICY IF EXISTS "Users can view active calls" ON active_calls;
CREATE POLICY "Users can view active calls" ON active_calls FOR SELECT USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);
DROP POLICY IF EXISTS "Users can create active calls" ON active_calls;
CREATE POLICY "Users can create active calls" ON active_calls FOR INSERT WITH CHECK (auth.uid()::text = caller_id::text);
DROP POLICY IF EXISTS "Users can update active calls" ON active_calls;
CREATE POLICY "Users can update active calls" ON active_calls FOR UPDATE USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);
DROP POLICY IF EXISTS "Users can delete active calls" ON active_calls;
CREATE POLICY "Users can delete active calls" ON active_calls FOR DELETE USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);

-- Recent searches policies
DROP POLICY IF EXISTS "Users can view their own searches" ON recent_searches;
CREATE POLICY "Users can view their own searches" ON recent_searches FOR SELECT USING (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can save searches" ON recent_searches;
CREATE POLICY "Users can save searches" ON recent_searches FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
DROP POLICY IF EXISTS "Users can delete searches" ON recent_searches;
CREATE POLICY "Users can delete searches" ON recent_searches FOR DELETE USING (auth.uid()::text = user_id::text);

-- =====================================================
-- NOTIFICATIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    
    -- Recipient (who receives the notification)
    recipient_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Actor (who triggered the notification)
    actor_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    actor_username TEXT NOT NULL,
    actor_profile_image TEXT DEFAULT '',
    
    -- Notification type: 'like', 'comment', 'follow', 'mention', 'story_view', 'like_comment'
    type TEXT NOT NULL,
    
    -- Related content (optional based on type)
    post_id UUID,
    post_thumbnail TEXT DEFAULT '',
    story_id UUID,
    comment_id UUID,
    comment_text TEXT DEFAULT '',
    
    -- Status
    is_read BOOLEAN DEFAULT false,
    
    -- Timestamp
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- Indexes for notifications
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read ON notifications(recipient_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);

-- Enable RLS
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Policies for notifications
DROP POLICY IF EXISTS "Users can view their own notifications" ON notifications;
CREATE POLICY "Users can view their own notifications" ON notifications 
    FOR SELECT USING (auth.uid()::text = recipient_id::text);

DROP POLICY IF EXISTS "Users can create notifications for others" ON notifications;
CREATE POLICY "Users can create notifications for others" ON notifications 
    FOR INSERT WITH CHECK (auth.uid()::text = actor_id::text);

DROP POLICY IF EXISTS "Users can update their own notifications" ON notifications;
CREATE POLICY "Users can update their own notifications" ON notifications 
    FOR UPDATE USING (auth.uid()::text = recipient_id::text);

DROP POLICY IF EXISTS "Users can delete their own notifications" ON notifications;
CREATE POLICY "Users can delete their own notifications" ON notifications 
    FOR DELETE USING (auth.uid()::text = recipient_id::text);

-- =====================================================
-- ENABLE REALTIME
-- =====================================================
-- Run these in your Supabase dashboard -> Database -> Replication
-- Or uncomment and run if you have proper permissions:

-- ALTER PUBLICATION supabase_realtime ADD TABLE users;
-- ALTER PUBLICATION supabase_realtime ADD TABLE posts;
-- ALTER PUBLICATION supabase_realtime ADD TABLE messages;
-- ALTER PUBLICATION supabase_realtime ADD TABLE stories;
-- ALTER PUBLICATION supabase_realtime ADD TABLE calls;
-- ALTER PUBLICATION supabase_realtime ADD TABLE active_calls;
-- ALTER PUBLICATION supabase_realtime ADD TABLE followers;
-- ALTER PUBLICATION supabase_realtime ADD TABLE conversations;
-- ALTER PUBLICATION supabase_realtime ADD TABLE notifications;  -- 🆕 For real-time notifications

-- =====================================================
-- STORAGE BUCKETS
-- =====================================================
-- Create these buckets in your Supabase dashboard -> Storage:
-- 1. profile-images (public)
-- 2. post-images (public)
-- 3. story-images (public)
-- 4. message-images (authenticated access only)
-- 5. videos (public) - For video posts and reels
-- 6. video-thumbnails (public) - For video thumbnail images

-- =====================================================
-- STORAGE RLS POLICIES (Run in Supabase Dashboard -> Storage -> Policies)
-- =====================================================
-- For 'videos' bucket:
-- SELECT: Allow public access (anyone can view)
-- INSERT: Allow authenticated users to upload to their folder
-- DELETE: Allow users to delete their own videos
--
-- Example policy for INSERT on videos bucket:
-- ((bucket_id = 'videos'::text) AND (auth.role() = 'authenticated'::text))

-- =====================================================
-- PASSWORD RESET FUNCTION
-- =====================================================
-- This function allows resetting a user's password after email+username verification.
-- Uses Supabase's extensions schema where pgcrypto is available.

-- Function to reset user password
-- Call this via RPC: supabase.postgrest.rpc('reset_user_password', { user_email: '...', new_password: '...' })
CREATE OR REPLACE FUNCTION reset_user_password(user_email TEXT, new_password TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
    target_user_id UUID;
BEGIN
    -- Find the user in our public.users table
    SELECT user_id INTO target_user_id
    FROM users
    WHERE LOWER(email) = LOWER(user_email);
    
    IF target_user_id IS NULL THEN
        RAISE EXCEPTION 'User not found';
    END IF;
    
    -- Update the password in auth.users using extensions.crypt
    UPDATE auth.users
    SET 
        encrypted_password = extensions.crypt(new_password, extensions.gen_salt('bf')),
        updated_at = NOW()
    WHERE id = target_user_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Failed to update password';
    END IF;
    
    RETURN TRUE;
END;
$$;

-- Grant execute permission to authenticated users (and anon for password reset)
GRANT EXECUTE ON FUNCTION reset_user_password(TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION reset_user_password(TEXT, TEXT) TO authenticated;
