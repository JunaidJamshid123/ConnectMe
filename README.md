# ConnectMe

A modern Android social networking application built with Clean Architecture principles, featuring real-time messaging, video calls, stories, and social interactions.

## Features

- **Authentication** - Email/password login and registration with Firebase Auth
- **Real-time Messaging** - One-on-one chat with message read receipts
- **Video/Voice Calls** - Powered by Agora SDK
- **Stories** - Share ephemeral content with followers
- **Posts & Feed** - Create posts, like, comment, and save
- **User Profiles** - Follow/unfollow users, view posts and followers
- **Search** - Find users and recent search history
- **Push Notifications** - Firebase Cloud Messaging integration

## Tech Stack

- **Language**: Kotlin
- **Architecture**: Clean Architecture (Presentation → Domain → Data)
- **UI**: Android Views with ViewBinding
- **Dependency Injection**: Hilt 2.51.1
- **Async**: Kotlin Coroutines & Flow
- **Backend**: Firebase (Auth, Realtime Database, Storage, Cloud Messaging)
- **Video Calls**: Agora RTC SDK
- **State Management**: StateFlow + ViewModel
- **Image Loading**: Base64 encoding/decoding

## Architecture

```
app/
├── data/
│   ├── dto/           # Data Transfer Objects
│   ├── mapper/        # DTO to Domain model mappers
│   ├── remote/        # Firebase data sources
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
2. Add your `google-services.json` to the `app/` directory
3. Configure Agora App ID in `VideoCallActivity.kt`
4. Build and run

## Requirements

- Android Studio Hedgehog or newer
- Min SDK: 23 (Android 6.0)
- Target SDK: 35
- JDK 17

## Dependencies

- AndroidX Core & AppCompat
- Material Design Components
- Firebase Auth, Database, Storage, Messaging
- Hilt for DI
- Kotlin Coroutines
- Agora RTC SDK
- CircleImageView

## License

This project is for educational purposes.

## Author

Junaid Jamshid
