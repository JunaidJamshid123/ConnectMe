# ConnectMe

A modern Android social networking application built with Clean Architecture principles, featuring real-time messaging, video calls, stories, and social interactions.

## Features

- **Authentication** - Email/password login and registration with Supabase Auth
- **Real-time Messaging** - One-on-one chat with message read receipts
- **Video/Voice Calls** - Powered by Agora SDK
- **Stories** - Share ephemeral content with followers
- **Posts & Feed** - Create posts, like, comment, and save
- **User Profiles** - Follow/unfollow users, view posts and followers
- **Search** - Find users and recent search history
- **Real-time Updates** - Supabase Realtime for live data synchronization

## Tech Stack

- **Language**: Kotlin
- **Architecture**: Clean Architecture (Presentation → Domain → Data)
- **UI**: Android Views with ViewBinding
- **Dependency Injection**: Hilt 2.51.1
- **Async**: Kotlin Coroutines & Flow
- **Backend**: Supabase (Auth, PostgreSQL Database, Storage, Realtime)
- **Video Calls**: Agora RTC SDK
- **State Management**: StateFlow + ViewModel
- **Image Loading**: Base64 encoding/decoding

## Architecture

```
app/
├── data/
│   ├── dto/           # Data Transfer Objects
│   ├── mapper/        # DTO to Domain model mappers
│   ├── remote/        # Supabase data sources
│   └── repository/    # Repository implementations
├── di/                # Hilt dependency injection modules
├── domain/
│   ├── model/         # Domain entities
│   ├── repository/    # Repository interfaces
│   └── usecase/       # Business logic use cases
├── presentation/
│   ├── auth/          # Login & SignUp
│   ├── call/          # Video/Voice calls
│   ├── chat/          # Messaging
│   ├── contacts/      # Contacts list
│   ├── follow/        # Followers/Following
│   ├── home/          # Main feed
│   ├── main/          # Main activity
│   ├── post/          # Post creation & details
│   ├── profile/       # User profiles
│   ├── search/        # User search
│   └── story/         # Stories
└── util/              # Utilities (Resource wrapper, etc.)
```

## Setup

1. Clone the repository
2. Create a Supabase project at https://supabase.com
3. Run the SQL schema from `supabase_schema.sql` in your Supabase SQL Editor
4. Update `SupabaseConfig.kt` with your Supabase URL and anon key:
   ```kotlin
   const val SUPABASE_URL = "https://your-project-id.supabase.co"
   const val SUPABASE_ANON_KEY = "your-anon-key"
   ```
5. Create the following storage buckets in Supabase (set to public):
   - `profile-images`
   - `post-images`
   - `story-images`
   - `message-images`
6. Enable Realtime for all tables in Supabase Dashboard
7. Configure Agora App ID in `Constants.kt`
8. Build and run

## Requirements

- Android Studio Hedgehog or newer
- Min SDK: 23 (Android 6.0)
- Target SDK: 35
- JDK 17

## Dependencies

- AndroidX Core & AppCompat
- Material Design Components
- Supabase Kotlin SDK (Auth, Postgrest, Storage, Realtime)
- Ktor Client
- Hilt for DI
- Kotlin Coroutines & Serialization
- Agora RTC SDK
- CircleImageView

## License

This project is for educational purposes.

## Author

Junaid Jamshid
