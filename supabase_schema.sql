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
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_posts_post_id ON posts(post_id);
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_timestamp ON posts(timestamp DESC);

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
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE story_viewers ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE active_calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE recent_searches ENABLE ROW LEVEL SECURITY;

-- Users policies
CREATE POLICY "Users are viewable by everyone" ON users FOR SELECT USING (true);
CREATE POLICY "Users can update their own profile" ON users FOR UPDATE USING (auth.uid()::text = user_id::text);
CREATE POLICY "Users can insert their own profile" ON users FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- Posts policies
CREATE POLICY "Posts are viewable by everyone" ON posts FOR SELECT USING (true);
CREATE POLICY "Users can create their own posts" ON posts FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
CREATE POLICY "Users can delete their own posts" ON posts FOR DELETE USING (auth.uid()::text = user_id::text);

-- Likes policies
CREATE POLICY "Likes are viewable by everyone" ON likes FOR SELECT USING (true);
CREATE POLICY "Users can like posts" ON likes FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
CREATE POLICY "Users can unlike posts" ON likes FOR DELETE USING (auth.uid()::text = user_id::text);

-- Comments policies
CREATE POLICY "Comments are viewable by everyone" ON comments FOR SELECT USING (true);
CREATE POLICY "Users can create comments" ON comments FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
CREATE POLICY "Users can delete their own comments" ON comments FOR DELETE USING (auth.uid()::text = user_id::text);

-- Stories policies
CREATE POLICY "Stories are viewable by everyone" ON stories FOR SELECT USING (true);
CREATE POLICY "Users can create their own stories" ON stories FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
CREATE POLICY "Users can delete their own stories" ON stories FOR DELETE USING (auth.uid()::text = user_id::text);

-- Story viewers policies
CREATE POLICY "Story viewers are viewable by story owner" ON story_viewers FOR SELECT USING (true);
CREATE POLICY "Users can mark stories as viewed" ON story_viewers FOR INSERT WITH CHECK (auth.uid()::text = viewer_id::text);

-- Followers policies
CREATE POLICY "Followers are viewable by everyone" ON followers FOR SELECT USING (true);
CREATE POLICY "Users can follow others" ON followers FOR INSERT WITH CHECK (auth.uid()::text = follower_id::text);
CREATE POLICY "Users can unfollow others" ON followers FOR DELETE USING (auth.uid()::text = follower_id::text);

-- Messages policies
CREATE POLICY "Users can view their own messages" ON messages FOR SELECT USING (
    auth.uid()::text = sender_id::text OR auth.uid()::text = receiver_id::text
);
CREATE POLICY "Users can send messages" ON messages FOR INSERT WITH CHECK (auth.uid()::text = sender_id::text);
CREATE POLICY "Users can update their own messages" ON messages FOR UPDATE USING (auth.uid()::text = sender_id::text);

-- Conversations policies
CREATE POLICY "Users can view their own conversations" ON conversations FOR SELECT USING (
    auth.uid()::text = participant_1::text OR auth.uid()::text = participant_2::text
);
CREATE POLICY "Users can create conversations" ON conversations FOR INSERT WITH CHECK (
    auth.uid()::text = participant_1::text OR auth.uid()::text = participant_2::text
);
CREATE POLICY "Users can update their conversations" ON conversations FOR UPDATE USING (
    auth.uid()::text = participant_1::text OR auth.uid()::text = participant_2::text
);

-- Calls policies
CREATE POLICY "Users can view their own calls" ON calls FOR SELECT USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);
CREATE POLICY "Users can create calls" ON calls FOR INSERT WITH CHECK (auth.uid()::text = caller_id::text);
CREATE POLICY "Users can update their calls" ON calls FOR UPDATE USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);

-- Active calls policies
CREATE POLICY "Users can view active calls" ON active_calls FOR SELECT USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);
CREATE POLICY "Users can create active calls" ON active_calls FOR INSERT WITH CHECK (auth.uid()::text = caller_id::text);
CREATE POLICY "Users can update active calls" ON active_calls FOR UPDATE USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);
CREATE POLICY "Users can delete active calls" ON active_calls FOR DELETE USING (
    auth.uid()::text = caller_id::text OR auth.uid()::text = receiver_id::text
);

-- Recent searches policies
CREATE POLICY "Users can view their own searches" ON recent_searches FOR SELECT USING (auth.uid()::text = user_id::text);
CREATE POLICY "Users can save searches" ON recent_searches FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);
CREATE POLICY "Users can delete searches" ON recent_searches FOR DELETE USING (auth.uid()::text = user_id::text);

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

-- =====================================================
-- STORAGE BUCKETS
-- =====================================================
-- Create these buckets in your Supabase dashboard -> Storage:
-- 1. profile-images (public)
-- 2. post-images (public)
-- 3. story-images (public)
-- 4. message-images (authenticated access only)
