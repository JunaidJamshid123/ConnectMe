# 📖 ConnectMe - Complete Codebase Analysis

> A comprehensive technical deep-dive into the ConnectMe Android application, covering architecture, flow diagrams, Kotlin patterns, and implementation details.

---

## 📋 Table of Contents

1. [Application Overview](#1-application-overview)
2. [Project Structure](#2-project-structure)
3. [Application Startup Flow](#3-application-startup-flow)
4. [Architecture Deep-Dive](#4-architecture-deep-dive)
5. [Screen-by-Screen Analysis](#5-screen-by-screen-analysis)
6. [Data Layer Implementation](#6-data-layer-implementation)
7. [Kotlin Patterns & Features](#7-kotlin-patterns--features)
8. [Dependency Injection](#8-dependency-injection)
9. [Database Schema](#9-database-schema)
10. [Key Algorithms & Logic](#10-key-algorithms--logic)
11. [Recommended Reading Order](#11-recommended-reading-order)

---

## 1. Application Overview

### What is ConnectMe?
ConnectMe is a **full-featured Instagram clone** built with modern Android development practices. It's a production-ready social media application that demonstrates enterprise-level Android architecture.

### Core Features
| Feature | Description | Key Files |
|---------|-------------|-----------|
| **Authentication** | Email/password login via Supabase | `presentation/auth/` |
| **Posts & Feed** | Image/video posts with carousels | `presentation/home/`, `presentation/post/` |
| **Stories** | 24-hour ephemeral content | `presentation/story/` |
| **Reels** | Short-form vertical videos | `presentation/reels/` |
| **Real-time Chat** | Instant messaging with Supabase Realtime | `presentation/chat/` |
| **Video Calls** | Agora SDK integration | `presentation/call/` |
| **Profiles** | User profiles with posts grid | `presentation/profile/` |

### Tech Stack Summary
```
Language:           Kotlin 2.1.0
Min SDK:            24 (Android 7.0)
Target SDK:         35 (Android 15)
Architecture:       Clean Architecture + MVVM
DI:                 Hilt 2.53
Backend:            Supabase (PostgreSQL + Auth + Storage + Realtime)
Networking:         Ktor Client 3.0.3
Video Player:       Media3 ExoPlayer 1.2.1
Video Calls:        Agora RTC SDK 4.5.1
Image Loading:      Glide 4.15.1
```

---

## 2. Project Structure

```
app/src/main/java/com/junaidjamshid/i211203/
│
├── 📄 ConnectMeApplication.kt      # Application class (Hilt + Lifecycle)
├── 📄 splashScreen.kt              # Entry point
│
├── 📁 presentation/                # UI LAYER (Activities, Fragments, ViewModels)
│   ├── auth/                       # Login, SignUp, ForgotPassword
│   │   ├── LoginActivity.kt
│   │   ├── LoginViewModel.kt
│   │   ├── LoginUiState.kt
│   │   ├── SignUpActivity.kt
│   │   └── ForgotPasswordActivity.kt
│   │
│   ├── main/                       # Main container with bottom nav
│   │   ├── MainActivityNew.kt
│   │   └── MainViewModel.kt
│   │
│   ├── home/                       # Home feed (posts + stories)
│   │   ├── HomeFragmentNew.kt
│   │   ├── HomeViewModel.kt
│   │   ├── HomeUiState.kt
│   │   ├── adapter/
│   │   │   ├── HomeFeedAdapter.kt
│   │   │   └── StoryAdapterNew.kt
│   │   └── video/
│   │       ├── ExoPlayerPool.kt
│   │       └── VideoAutoPlayManager.kt
│   │
│   ├── post/                       # Post creation & details
│   │   ├── AddPostFragmentNew.kt
│   │   ├── PostDetailActivity.kt
│   │   └── CommentsActivity.kt
│   │
│   ├── story/                      # Story creation & display
│   │   ├── AddStoryActivity.kt
│   │   └── StoryDisplayActivityNew.kt
│   │
│   ├── reels/                      # Reels (TikTok-style)
│   │   ├── ReelsFragment.kt
│   │   ├── ReelsViewModel.kt
│   │   └── adapter/ReelsAdapter.kt
│   │
│   ├── chat/                       # Real-time messaging
│   │   ├── ChatActivity.kt
│   │   └── ChatViewModel.kt
│   │
│   ├── call/                       # Video calls (Agora)
│   │   └── VideoCallActivity.kt
│   │
│   ├── profile/                    # User profiles
│   │   ├── ProfileFragmentNew.kt
│   │   ├── ProfileViewModel.kt
│   │   ├── EditProfileActivity.kt
│   │   └── UserProfileActivity.kt
│   │
│   ├── search/                     # Search functionality
│   │   └── SearchFragmentNew.kt
│   │
│   ├── discover/                   # Discover people
│   │   └── DiscoverPeopleActivity.kt
│   │
│   ├── follow/                     # Followers/Following
│   │   ├── FollowersActivity.kt
│   │   └── FollowingActivity.kt
│   │
│   ├── messages/                   # DM list
│   │   └── DmsActivity.kt
│   │
│   └── common/                     # Shared UI components
│       └── animation/
│           └── StaggeredFadeAnimator.kt
│
├── 📁 domain/                      # DOMAIN LAYER (Business Logic)
│   ├── model/                      # Domain entities (pure Kotlin)
│   │   ├── User.kt
│   │   ├── Post.kt
│   │   ├── Story.kt
│   │   ├── Message.kt
│   │   ├── Call.kt
│   │   ├── Comment.kt
│   │   └── MediaType.kt
│   │
│   ├── repository/                 # Repository interfaces (contracts)
│   │   ├── AuthRepository.kt
│   │   ├── UserRepository.kt
│   │   ├── PostRepository.kt
│   │   ├── StoryRepository.kt
│   │   ├── MessageRepository.kt
│   │   └── CallRepository.kt
│   │
│   └── usecase/                    # Use cases (single responsibility)
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   ├── SignUpUseCase.kt
│       │   ├── LogoutUseCase.kt
│       │   ├── GetCurrentUserUseCase.kt
│       │   └── ForgotPasswordUseCase.kt
│       ├── post/
│       │   ├── CreatePostUseCase.kt
│       │   ├── GetFeedPostsUseCase.kt
│       │   ├── LikePostUseCase.kt
│       │   └── UnlikePostUseCase.kt
│       ├── story/
│       │   └── GetStoriesUseCase.kt
│       ├── user/
│       │   └── ...
│       └── message/
│           └── ...
│
├── 📁 data/                        # DATA LAYER (Implementation)
│   ├── dto/                        # Data Transfer Objects
│   │   ├── UserDto.kt
│   │   ├── PostDto.kt
│   │   ├── StoryDto.kt
│   │   ├── MessageDto.kt
│   │   └── CallDto.kt
│   │
│   ├── mapper/                     # DTO ↔ Domain mappers
│   │   ├── UserMapper.kt
│   │   ├── PostMapper.kt
│   │   ├── StoryMapper.kt
│   │   ├── MessageMapper.kt
│   │   └── CallMapper.kt
│   │
│   ├── remote/supabase/            # Supabase data sources
│   │   ├── SupabaseClientProvider.kt   # Singleton client
│   │   ├── SupabaseConfig.kt           # URLs & keys
│   │   ├── SupabaseAuthDataSource.kt
│   │   ├── SupabaseUserDataSource.kt
│   │   ├── SupabasePostDataSource.kt
│   │   ├── SupabaseStoryDataSource.kt
│   │   ├── SupabaseMessageDataSource.kt
│   │   ├── SupabaseCallDataSource.kt
│   │   └── SupabaseStorageDataSource.kt
│   │
│   └── repository/                 # Repository implementations
│       ├── AuthRepositoryImpl.kt
│       ├── UserRepositoryImpl.kt
│       ├── PostRepositoryImpl.kt
│       ├── StoryRepositoryImpl.kt
│       ├── MessageRepositoryImpl.kt
│       └── CallRepositoryImpl.kt
│
├── 📁 di/                          # DEPENDENCY INJECTION (Hilt)
│   ├── AppModule.kt                # Provides SupabaseClient
│   └── RepositoryModule.kt         # Binds interfaces → implementations
│
└── 📁 util/                        # UTILITIES
    ├── Resource.kt                 # Success/Error/Loading wrapper
    ├── Constants.kt
    └── VideoCompressorUtil.kt
```

---

## 3. Application Startup Flow

### Flow Diagram
```
┌─────────────────────────────────────────────────────────────────┐
│                        APP LAUNCH                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  ConnectMeApplication.onCreate()                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 1. SupabaseClientProvider.initialize(this)                  ││
│  │ 2. ProcessLifecycleOwner.get().lifecycle.addObserver(this)  ││
│  │    → Tracks app foreground/background for online status     ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  SplashScreen.onCreate()                                         │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ lifecycleScope.launch {                                      ││
│  │     delay(1500)  // Show branding                           ││
│  │                                                              ││
│  │     // Wait for Supabase session (max 3 seconds)            ││
│  │     val sessionStatus = withTimeoutOrNull(3000) {           ││
│  │         SupabaseClientProvider.client.auth.sessionStatus    ││
│  │             .first { it !is SessionStatus.Initializing }    ││
│  │     }                                                        ││
│  │                                                              ││
│  │     val isLoggedIn = sessionStatus is Authenticated         ││
│  │                                                              ││
│  │     if (isLoggedIn) → MainActivityNew                       ││
│  │     else → LoginActivity                                     ││
│  │ }                                                            ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┴───────────────────┐
          ▼                                       ▼
┌─────────────────────┐             ┌─────────────────────────────┐
│   LoginActivity     │             │   MainActivityNew            │
│   (Not logged in)   │             │   (Already authenticated)   │
└─────────────────────┘             └─────────────────────────────┘
```

### Code: SplashScreen.kt
```kotlin
@AndroidEntryPoint
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash_screen)

        lifecycleScope.launch {
            // Wait minimum splash duration for branding
            delay(1500)
            
            // Wait for session to be loaded from storage (with timeout)
            val sessionStatus = withTimeoutOrNull(3000) {
                SupabaseClientProvider.client.auth.sessionStatus.first { status ->
                    status !is SessionStatus.Initializing
                }
            }
            
            val isLoggedIn = when (sessionStatus) {
                is SessionStatus.Authenticated -> true
                else -> false
            }
            
            if (isLoggedIn) {
                startActivity(Intent(this@SplashScreen, MainActivityNew::class.java))
            } else {
                startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
            }
            finish()
        }
    }
}
```

### Code: ConnectMeApplication.kt
```kotlin
@HiltAndroidApp
class ConnectMeApplication : Application(), DefaultLifecycleObserver {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super<Application>.onCreate()
        // Initialize Supabase client with context for session persistence
        SupabaseClientProvider.initialize(this)
        
        // Register lifecycle observer for online status management
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground - set online status to true
        updateOnlineStatus(true)
    }
    
    override fun onStop(owner: LifecycleOwner) {
        // App went to background - set online status to false
        updateOnlineStatus(false)
    }
    
    private fun updateOnlineStatus(isOnline: Boolean) {
        applicationScope.launch {
            val currentUserId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            if (currentUserId != null) {
                userRepository.updateOnlineStatus(currentUserId, isOnline)
            }
        }
    }
}
```

---

## 4. Architecture Deep-Dive

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                               │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  Activities/Fragments    →    ViewModels    →    UiState         │   │
│  │  (UI events)                  (business)        (data class)     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                     │                                    │
│                                     │ Uses                               │
│                                     ▼                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                           DOMAIN LAYER                                   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  UseCases → Repository Interfaces → Domain Models                │   │
│  │  (business logic)   (contracts)        (pure Kotlin)             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                     │                                    │
│                                     │ Implemented by                     │
│                                     ▼                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                            DATA LAYER                                    │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  RepositoryImpl → DataSources → DTOs → Mappers → Network/DB      │   │
│  │                                                    (Supabase)     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Example: Login

```
┌─────────────────────────────────────────────────────────────────────────┐
│  1. USER TAPS LOGIN BUTTON                                               │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  LoginActivity.kt                                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ binding.LoginBtn.setOnClickListener {                               ││
│  │     viewModel.onEmailChange(email)                                  ││
│  │     viewModel.onPasswordChange(password)                            ││
│  │     viewModel.onLoginClick()                                        ││
│  │ }                                                                    ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  LoginViewModel.kt                                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ @HiltViewModel                                                       ││
│  │ class LoginViewModel @Inject constructor(                            ││
│  │     private val loginUseCase: LoginUseCase                           ││
│  │ ) : ViewModel() {                                                    ││
│  │                                                                      ││
│  │     fun onLoginClick() {                                             ││
│  │         viewModelScope.launch {                                      ││
│  │             _uiState.update { it.copy(isLoading = true) }            ││
│  │                                                                      ││
│  │             when (val result = loginUseCase(email, password)) {      ││
│  │                 is Resource.Success → navigate to main               ││
│  │                 is Resource.Error → show error                       ││
│  │             }                                                        ││
│  │         }                                                            ││
│  │     }                                                                ││
│  │ }                                                                    ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  LoginUseCase.kt (Domain Layer)                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ class LoginUseCase @Inject constructor(                              ││
│  │     private val authRepository: AuthRepository                       ││
│  │ ) {                                                                  ││
│  │     suspend operator fun invoke(email: String, password: String) {   ││
│  │         // Validation logic                                          ││
│  │         if (email.isBlank()) return Resource.Error("Email required") ││
│  │         if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())        ││
│  │             return Resource.Error("Invalid email")                   ││
│  │                                                                      ││
│  │         return authRepository.login(email, password)                 ││
│  │     }                                                                ││
│  │ }                                                                    ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  AuthRepositoryImpl.kt (Data Layer)                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ @Singleton                                                           ││
│  │ class AuthRepositoryImpl @Inject constructor(                        ││
│  │     private val authDataSource: SupabaseAuthDataSource               ││
│  │ ) : AuthRepository {                                                 ││
│  │                                                                      ││
│  │     override suspend fun login(email, password): Resource<User> {    ││
│  │         return try {                                                 ││
│  │             val userId = authDataSource.login(email, password)       ││
│  │             val userDto = userDataSource.getUserById(userId)         ││
│  │             Resource.Success(userDto.toDomain())                     ││
│  │         } catch (e: Exception) {                                     ││
│  │             Resource.Error(e.message ?: "Login failed")              ││
│  │         }                                                            ││
│  │     }                                                                ││
│  │ }                                                                    ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  SupabaseAuthDataSource.kt                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ @Singleton                                                           ││
│  │ class SupabaseAuthDataSource @Inject constructor(                    ││
│  │     private val supabaseClient: SupabaseClient                       ││
│  │ ) {                                                                  ││
│  │     suspend fun login(email: String, password: String): String {     ││
│  │         supabaseClient.auth.signInWith(Email) {                      ││
│  │             this.email = email                                       ││
│  │             this.password = password                                 ││
│  │         }                                                            ││
│  │         return supabaseClient.auth.currentUserOrNull()?.id           ││
│  │             ?: throw Exception("Login failed")                       ││
│  │     }                                                                ││
│  │ }                                                                    ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Supabase Cloud                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ POST /auth/v1/token?grant_type=password                              ││
│  │ { "email": "...", "password": "..." }                                ││
│  │                                                                      ││
│  │ Response: { "access_token": "...", "user": { "id": "..." } }         ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

### State Management Pattern

```kotlin
// UiState - Immutable data class representing screen state
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

// ViewModel - Manages state with StateFlow
@HiltViewModel
class LoginViewModel @Inject constructor(...) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // Immutable state updates using copy()
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }
    
    fun onLoginClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // ... login logic
            _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
        }
    }
}

// Activity/Fragment - Observes state with collectAsState/collect
private fun observeUiState() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state ->
                // Update UI based on state
                binding.LoginBtn.isEnabled = !state.isLoading
                binding.progressBar.isVisible = state.isLoading
                
                if (state.isLoggedIn) navigateToMain()
                state.error?.let { showError(it) }
            }
        }
    }
}
```

---

## 5. Screen-by-Screen Analysis

### 5.1 Main Navigation Structure

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MainActivityNew                                  │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                         Fragment Container                         │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │  HomeFragmentNew    (nav_home)                               │  │  │
│  │  │  SearchFragmentNew  (nav_search)                             │  │  │
│  │  │  AddPostFragmentNew (nav_add_post)                           │  │  │
│  │  │  ReelsFragment      (nav_contacts)                           │  │  │
│  │  │  ProfileFragmentNew (nav_profile)                            │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  Bottom Navigation Bar                                             │  │
│  │  [🏠 Home] [🔍 Search] [➕ Add] [🎬 Reels] [👤 Profile]           │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Home Screen (HomeFragmentNew)

**Purpose:** Display feed of posts from followed users + stories

**Key Components:**
- Stories RecyclerView (horizontal)
- Posts RecyclerView (vertical with video auto-play)
- Pull-to-refresh
- Shimmer loading states

**Data Flow:**
```
HomeFragmentNew
    │
    ├── observes → HomeViewModel.uiState
    │                   │
    │                   ├── posts: List<Post>
    │                   ├── stories: List<Story>
    │                   ├── suggestedUsers: List<User>
    │                   ├── isLoadingPosts: Boolean
    │                   └── isRefreshing: Boolean
    │
    └── events →
        ├── onLikeClick(postId) → viewModel.onLikePost(postId)
        ├── onCommentClick(postId) → navigate to CommentsActivity
        ├── onProfileClick(userId) → navigate to UserProfileActivity
        └── onRefresh() → viewModel.onRefresh()
```

**HomeUiState.kt:**
```kotlin
data class HomeUiState(
    val currentUser: User? = null,
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val suggestedUsers: List<User> = emptyList(),
    val followingUserIds: Set<String> = emptySet(),
    val currentUserHasStory: Boolean = false,
    val isLoadingPosts: Boolean = false,
    val isLoadingStories: Boolean = false,
    val isRefreshing: Boolean = false,
    val postsError: String? = null,
    val storiesError: String? = null
)
```

### 5.3 Reels Screen (ReelsFragment)

**Purpose:** Full-screen vertical video feed (TikTok-style)

**Key Features:**
- ViewPager2 with vertical orientation
- Auto-play on page change
- Pause during scroll
- Video view tracking

**Implementation Details:**
```kotlin
binding.reelsViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
        // Pause previous video
        if (currentPosition != position) {
            reelsAdapter.pauseAt(currentPosition)
        }
        currentPosition = position
        
        // Play new video
        reelsAdapter.playAt(position)
        
        // Record view for analytics
        viewModel.recordVideoView(reels[position].postId)
    }
    
    override fun onPageScrollStateChanged(state: Int) {
        // Pause during drag to prevent audio overlap
        if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
            reelsAdapter.pauseAt(currentPosition)
        } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
            reelsAdapter.playAt(currentPosition)
        }
    }
})
```

### 5.4 Profile Screen (ProfileFragmentNew)

**Purpose:** Display user profile with posts grid, reels, and saved posts

**Tab Structure:**
```
┌─────────────────────────────────────────────────────────────────┐
│  Profile Header (image, bio, followers, following)               │
├─────────────────────────────────────────────────────────────────┤
│  Story Highlights (horizontal RecyclerView)                      │
├─────────────────────────────────────────────────────────────────┤
│  [📷 Posts] [🎬 Reels] [🔖 Saved]  ← Tab buttons                 │
├─────────────────────────────────────────────────────────────────┤
│  Grid RecyclerView (3 columns)                                   │
│  ┌─────┬─────┬─────┐                                            │
│  │     │     │     │                                            │
│  ├─────┼─────┼─────┤                                            │
│  │     │     │     │                                            │
│  └─────┴─────┴─────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Data Layer Implementation

### 6.1 Supabase Client Setup

```kotlin
object SupabaseClientProvider {
    private var _client: SupabaseClient? = null
    
    fun initialize(context: Context) {
        _client = createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                autoLoadFromStorage = true    // Persists session
                alwaysAutoRefresh = true      // Auto-refresh tokens
            }
            install(Postgrest)                // Database operations
            install(Storage)                  // File uploads
            install(Realtime)                 // WebSocket for live updates
        }
    }
    
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("Not initialized")
}
```

### 6.2 DTO to Domain Mapping

**PostDto.kt (Data Layer):**
```kotlin
data class PostDto(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val caption: String = "",
    val location: String = "",
    val musicName: String = "",
    val musicArtist: String = "",
    val timestamp: Long = 0,
    val likes: MutableMap<String, Boolean> = mutableMapOf(),
    val comments: MutableList<CommentDto> = mutableListOf(),
    // Video fields
    val mediaType: String = "image",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val videoDuration: Int = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val aspectRatio: Float = 1f,
    val viewsCount: Int = 0
)
```

**Post.kt (Domain Layer):**
```kotlin
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val caption: String = "",
    val location: String = "",
    val musicName: String = "",
    val musicArtist: String = "",
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val isSavedByCurrentUser: Boolean = false,
    val mediaType: MediaType = MediaType.IMAGE,
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val videoDuration: Int = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val aspectRatio: Float = 1f,
    val viewsCount: Int = 0
) {
    // Computed properties
    val isCarousel: Boolean get() = mediaType == MediaType.IMAGE && imageUrls.size > 1
    val isVideo: Boolean get() = mediaType == MediaType.VIDEO || mediaType == MediaType.REEL
    val isReel: Boolean get() = mediaType == MediaType.REEL
    
    val formattedDuration: String
        get() {
            if (videoDuration <= 0) return ""
            val totalSeconds = videoDuration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}
```

**PostMapper.kt:**
```kotlin
object PostMapper {
    fun PostDto.toDomain(currentUserId: String): Post {
        return Post(
            postId = postId,
            userId = userId,
            username = username,
            userProfileImage = userProfileImage,
            postImageUrl = postImageUrl,
            imageUrls = imageUrls,
            caption = caption,
            location = location,
            musicName = musicName,
            musicArtist = musicArtist,
            timestamp = timestamp,
            likesCount = likes.size,
            commentsCount = comments.size,
            isLikedByCurrentUser = likes.containsKey(currentUserId),
            isSavedByCurrentUser = false,
            mediaType = MediaType.fromString(mediaType),
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            videoDuration = videoDuration,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            aspectRatio = aspectRatio,
            viewsCount = viewsCount
        )
    }
}
```

### 6.3 Repository Implementation Pattern

```kotlin
@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postDataSource: SupabasePostDataSource,
    private val userDataSource: SupabaseUserDataSource
) : PostRepository {
    
    override fun getFeedPosts(userId: String): Flow<Resource<List<Post>>> {
        return postDataSource.getFeedPosts()
            .map { posts ->
                // Enrich posts with fresh user profile images
                val enrichedPosts = enrichPostsWithUserData(posts)
                
                // Get saved post IDs for current user
                val savedPostIds = try {
                    postDataSource.getSavedPostIds(userId).toSet()
                } catch (e: Exception) {
                    emptySet()
                }
                
                // Map DTOs to domain models
                Resource.Success(enrichedPosts.map { dto -> 
                    dto.toDomain(userId).copy(
                        isSavedByCurrentUser = savedPostIds.contains(dto.postId)
                    )
                }) as Resource<List<Post>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to load posts"))
            }
    }
}
```

---

## 7. Kotlin Patterns & Features

### 7.1 Coroutines & Flow

**Launching Coroutines in ViewModel:**
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(...) : ViewModel() {
    
    init {
        loadPosts()  // Called in init block
    }
    
    private fun loadPosts() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingPosts = true) }
                
                getFeedPostsUseCase(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update { 
                                it.copy(posts = result.data ?: emptyList(), isLoadingPosts = false) 
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(postsError = result.message, isLoadingPosts = false) }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoadingPosts = true) }
                        }
                    }
                }
            }
        }
    }
}
```

**Collecting Flow in UI:**
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            handleUiState(state)
        }
    }
}
```

### 7.2 Extension Functions

**Custom Extensions (not shown in code, but typical):**
```kotlin
// String extensions
fun String.isValidEmail(): Boolean = 
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

// View extensions
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

// ImageView extensions with Glide
fun ImageView.loadUrl(url: String) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.placeholder)
        .into(this)
}
```

### 7.3 Sealed Classes

**Resource.kt - Result Wrapper:**
```kotlin
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}

// Usage
when (result) {
    is Resource.Success -> { /* handle success */ }
    is Resource.Error -> { /* handle error */ }
    is Resource.Loading -> { /* show loading */ }
}
```

**MediaType.kt:**
```kotlin
enum class MediaType {
    IMAGE,
    VIDEO,
    REEL;
    
    companion object {
        fun fromString(type: String): MediaType {
            return when (type.lowercase()) {
                "video" -> VIDEO
                "reel" -> REEL
                else -> IMAGE
            }
        }
    }
}
```

### 7.4 Data Classes & Copy

```kotlin
// Immutable state updates
_uiState.update { currentState ->
    currentState.copy(
        posts = currentState.posts.map { post ->
            if (post.postId == postId) {
                post.copy(
                    isLikedByCurrentUser = !post.isLikedByCurrentUser,
                    likesCount = if (post.isLikedByCurrentUser) post.likesCount - 1 else post.likesCount + 1
                )
            } else post
        }
    )
}
```

### 7.5 Lazy Initialization

```kotlin
// Lazy fragment initialization
private val homeFragment by lazy { HomeFragmentNew() }
private val searchFragment by lazy { SearchFragmentNew() }
private val addPostFragment by lazy { AddPostFragmentNew() }
private val reelsFragment by lazy { ReelsFragment() }
private val profileFragment by lazy { ProfileFragmentNew() }

// Lazy repository access in Application
private val userRepository: UserRepository by lazy {
    EntryPointAccessors.fromApplication(this, UserRepositoryEntryPoint::class.java)
        .userRepository()
}
```

### 7.6 Scope Functions

```kotlin
// let - null safety
currentUserId?.let { userId ->
    loadPosts(userId)
}

// apply - builder pattern
binding.recyclerView.apply {
    layoutManager = LinearLayoutManager(context)
    adapter = feedAdapter
    setHasFixedSize(false)
    setItemViewCacheSize(10)
}

// also - side effects
result.also { Log.d(TAG, "Result: $it") }

// run - execute block and return result
val user = userRepository.run {
    getUserById(userId)
}
```

---

## 8. Dependency Injection

### 8.1 Hilt Setup

**Application Class:**
```kotlin
@HiltAndroidApp
class ConnectMeApplication : Application() { ... }
```

**Activity Annotation:**
```kotlin
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() { ... }
```

**ViewModel Annotation:**
```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() { ... }
```

### 8.2 Module Definitions

**AppModule.kt - Provides infrastructure:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClientProvider.client
    }
}
```

**RepositoryModule.kt - Binds interfaces:**
```kotlin
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
```

### 8.3 Injection Flow

```
                     Hilt Container (SingletonComponent)
                                   │
    ┌──────────────────────────────┼──────────────────────────────┐
    │                              │                              │
    ▼                              ▼                              ▼
SupabaseClient            SupabaseDataSources           Repository Interfaces
    │                              │                              │
    └───────────────┬──────────────┘                              │
                    ▼                                             │
            RepositoryImpl ◄──────────────────────────────────────┘
                    │
                    ▼
               UseCases
                    │
                    ▼
              ViewModels
                    │
                    ▼
          Activities/Fragments
```

---

## 9. Database Schema

### 9.1 Core Tables

```sql
-- Users Table
CREATE TABLE users (
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
    created_at BIGINT,
    last_seen BIGINT,
    vanish_mode_enabled BOOLEAN DEFAULT false
);

-- Posts Table
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    user_profile_image TEXT DEFAULT '',
    post_image_url TEXT NOT NULL,
    image_urls TEXT DEFAULT '[]',
    caption TEXT DEFAULT '',
    location TEXT DEFAULT '',
    music_name TEXT DEFAULT '',
    music_artist TEXT DEFAULT '',
    timestamp BIGINT,
    -- Video fields
    media_type TEXT DEFAULT 'image',  -- 'image', 'video', 'reel'
    video_url TEXT DEFAULT '',
    thumbnail_url TEXT DEFAULT '',
    video_duration INTEGER DEFAULT 0,
    video_width INTEGER DEFAULT 0,
    video_height INTEGER DEFAULT 0,
    aspect_ratio REAL DEFAULT 1.0,
    views_count INTEGER DEFAULT 0
);

-- Followers Table
CREATE TABLE followers (
    id BIGSERIAL PRIMARY KEY,
    follower_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at BIGINT,
    UNIQUE(follower_id, following_id)
);

-- Likes Table
CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at BIGINT,
    UNIQUE(post_id, user_id)
);

-- Stories Table
CREATE TABLE stories (
    id BIGSERIAL PRIMARY KEY,
    story_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    user_profile_image TEXT DEFAULT '',
    story_image_url TEXT NOT NULL,
    timestamp BIGINT,
    expiry_timestamp BIGINT  -- 24 hours from creation
);

-- Messages Table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    message_id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    content TEXT DEFAULT '',
    image_url TEXT DEFAULT '',
    timestamp BIGINT,
    is_read BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    message_type TEXT DEFAULT 'text'
);
```

### 9.2 Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   users     │       │   posts     │       │   stories   │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ user_id (PK)│◄──────│ user_id (FK)│       │ user_id (FK)│──────►│
│ username    │       │ post_id (PK)│       │ story_id(PK)│       │
│ email       │       │ caption     │       │ image_url   │       │
│ profile_pic │       │ image_url   │       │ expiry_time │       │
│ bio         │       │ media_type  │       └─────────────┘       │
│ online      │       │ video_url   │                             │
└─────────────┘       │ timestamp   │                             │
      │               └─────────────┘                             │
      │                     │                                     │
      │                     │ 1:N                                 │
      │                     ▼                                     │
      │               ┌─────────────┐                             │
      │               │   likes     │                             │
      │               ├─────────────┤                             │
      │               │ post_id (FK)│                             │
      │               │ user_id (FK)│◄────────────────────────────┘
      │               └─────────────┘
      │
      │ M:N (self-referencing)
      ▼
┌─────────────┐
│  followers  │
├─────────────┤
│ follower_id │
│ following_id│
└─────────────┘
```

---

## 10. Key Algorithms & Logic

### 10.1 Optimistic UI Updates (Likes)

```kotlin
fun onLikePost(postId: String) {
    val post = _uiState.value.posts.find { it.postId == postId } ?: return
    val isCurrentlyLiked = post.isLikedByCurrentUser
    val newLikesCount = if (isCurrentlyLiked) post.likesCount - 1 else post.likesCount + 1

    // 1. OPTIMISTIC UPDATE - Immediately update UI
    _uiState.update { state ->
        state.copy(
            posts = state.posts.map { p ->
                if (p.postId == postId) {
                    p.copy(
                        isLikedByCurrentUser = !isCurrentlyLiked,
                        likesCount = newLikesCount.coerceAtLeast(0)
                    )
                } else p
            }
        )
    }

    // 2. BACKGROUND OPERATION - Perform actual API call
    viewModelScope.launch {
        val result = if (isCurrentlyLiked) {
            unlikePostUseCase(postId, userId)
        } else {
            likePostUseCase(postId, userId)
        }
        
        // 3. REVERT IF FAILED - Rollback optimistic update
        if (result is Resource.Error) {
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { p ->
                        if (p.postId == postId) {
                            p.copy(
                                isLikedByCurrentUser = isCurrentlyLiked,  // Original
                                likesCount = post.likesCount              // Original
                            )
                        } else p
                    }
                )
            }
        }
    }
}
```

### 10.2 Video Auto-Play on Scroll

```kotlin
class VideoAutoPlayManager(
    private val recyclerView: RecyclerView,
    private val playerPool: ExoPlayerPool,
    private val onVideoViewTracked: (String) -> Unit
) {
    private var currentlyPlayingPosition: Int = -1
    
    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    findAndPlayVisibleVideo()
                }
            }
        })
    }
    
    private fun findAndPlayVisibleVideo() {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
        
        // Find the most visible video post
        for (position in firstVisible..lastVisible) {
            val post = getPostAtPosition(position)
            if (post?.isVideo == true) {
                if (position != currentlyPlayingPosition) {
                    pauseCurrent()
                    playAt(position)
                    currentlyPlayingPosition = position
                    onVideoViewTracked(post.postId)
                }
                break
            }
        }
    }
}
```

### 10.3 Session Persistence (Supabase Auth)

```kotlin
// In SupabaseClientProvider
fun initialize(context: Context) {
    _client = createSupabaseClient(...) {
        install(Auth) {
            autoLoadFromStorage = true    // Load session from SharedPreferences
            alwaysAutoRefresh = true      // Auto-refresh expired tokens
        }
    }
}

// In SplashScreen - Wait for session to load
val sessionStatus = withTimeoutOrNull(3000) {
    SupabaseClientProvider.client.auth.sessionStatus.first { status ->
        status !is SessionStatus.Initializing  // Wait until loaded
    }
}

val isLoggedIn = sessionStatus is SessionStatus.Authenticated
```

---

## 11. Recommended Reading Order

### Phase 1: Entry Point & Setup (15 minutes)
1. **[splashScreen.kt](app/src/main/java/com/junaidjamshid/i211203/splashScreen.kt)** - App entry, session check
2. **[ConnectMeApplication.kt](app/src/main/java/com/junaidjamshid/i211203/ConnectMeApplication.kt)** - Hilt setup, lifecycle
3. **[AndroidManifest.xml](app/src/main/AndroidManifest.xml)** - All activities declared

### Phase 2: Architecture Foundation (20 minutes)
4. **[di/AppModule.kt](app/src/main/java/com/junaidjamshid/i211203/di/AppModule.kt)** - Supabase provider
5. **[di/RepositoryModule.kt](app/src/main/java/com/junaidjamshid/i211203/di/RepositoryModule.kt)** - Interface bindings
6. **[util/Resource.kt](app/src/main/java/com/junaidjamshid/i211203/util/Resource.kt)** - Result wrapper

### Phase 3: Domain Layer (20 minutes)
7. **[domain/model/User.kt](app/src/main/java/com/junaidjamshid/i211203/domain/model/User.kt)** - User entity
8. **[domain/model/Post.kt](app/src/main/java/com/junaidjamshid/i211203/domain/model/Post.kt)** - Post entity with computed properties
9. **[domain/repository/AuthRepository.kt](app/src/main/java/com/junaidjamshid/i211203/domain/repository/AuthRepository.kt)** - Contract
10. **[domain/usecase/auth/LoginUseCase.kt](app/src/main/java/com/junaidjamshid/i211203/domain/usecase/auth/LoginUseCase.kt)** - Business logic

### Phase 4: Data Layer (25 minutes)
11. **[data/remote/supabase/SupabaseClientProvider.kt](app/src/main/java/com/junaidjamshid/i211203/data/remote/supabase/SupabaseClientProvider.kt)** - Client singleton
12. **[data/remote/supabase/SupabaseAuthDataSource.kt](app/src/main/java/com/junaidjamshid/i211203/data/remote/supabase/SupabaseAuthDataSource.kt)** - Auth API
13. **[data/dto/PostDto.kt](app/src/main/java/com/junaidjamshid/i211203/data/dto/PostDto.kt)** - Data transfer objects
14. **[data/mapper/PostMapper.kt](app/src/main/java/com/junaidjamshid/i211203/data/mapper/PostMapper.kt)** - DTO → Domain
15. **[data/repository/PostRepositoryImpl.kt](app/src/main/java/com/junaidjamshid/i211203/data/repository/PostRepositoryImpl.kt)** - Implementation

### Phase 5: Presentation Layer (30 minutes)
16. **[presentation/auth/LoginActivity.kt](app/src/main/java/com/junaidjamshid/i211203/presentation/auth/LoginActivity.kt)** - Login UI
17. **[presentation/auth/LoginViewModel.kt](app/src/main/java/com/junaidjamshid/i211203/presentation/auth/LoginViewModel.kt)** - Login state
18. **[presentation/main/MainActivityNew.kt](app/src/main/java/com/junaidjamshid/i211203/presentation/main/MainActivityNew.kt)** - Navigation
19. **[presentation/home/HomeFragmentNew.kt](app/src/main/java/com/junaidjamshid/i211203/presentation/home/HomeFragmentNew.kt)** - Feed UI
20. **[presentation/home/HomeViewModel.kt](app/src/main/java/com/junaidjamshid/i211203/presentation/home/HomeViewModel.kt)** - Feed logic

### Phase 6: Database & Advanced (15 minutes)
21. **[supabase_schema.sql](supabase_schema.sql)** - Full database schema
22. **[presentation/reels/ReelsFragment.kt](app/src/main/java/com/junaidjamshid/i211203/presentation/reels/ReelsFragment.kt)** - Video feed
23. **[presentation/profile/ProfileFragmentNew.kt](app/src/main/java/com/junaidjamshid/i211203/presentation/profile/ProfileFragmentNew.kt)** - Profile tabs

---

## 📚 Quick Reference

### Key Files by Feature

| Feature | Key Files |
|---------|-----------|
| **Authentication** | `LoginActivity`, `LoginViewModel`, `LoginUseCase`, `AuthRepositoryImpl`, `SupabaseAuthDataSource` |
| **Feed Posts** | `HomeFragmentNew`, `HomeViewModel`, `GetFeedPostsUseCase`, `PostRepositoryImpl`, `SupabasePostDataSource` |
| **Stories** | `AddStoryActivity`, `StoryDisplayActivityNew`, `GetStoriesUseCase`, `StoryRepositoryImpl` |
| **Reels** | `ReelsFragment`, `ReelsViewModel`, `ExoPlayerPool`, video-related Post methods |
| **Chat** | `ChatActivity`, `ChatViewModel`, `MessageRepositoryImpl`, `SupabaseMessageDataSource` |
| **Profile** | `ProfileFragmentNew`, `ProfileViewModel`, `UserRepositoryImpl`, `EditProfileActivity` |
| **Video Calls** | `VideoCallActivity`, Agora SDK integration |

### Architecture Summary

```
┌────────────────────────────────────────────────────────────────┐
│ Clean Architecture + MVVM                                       │
├────────────────────────────────────────────────────────────────┤
│ PRESENTATION: Activities/Fragments ↔ ViewModels ↔ UiState     │
│ DOMAIN:       UseCases → Repository Interfaces → Models        │
│ DATA:         RepositoryImpl → DataSources → DTOs → Supabase  │
│ DI:           Hilt (@HiltAndroidApp, @AndroidEntryPoint, etc.) │
└────────────────────────────────────────────────────────────────┘
```

---

**Generated:** March 2026  
**Author:** Codebase Analysis Tool  
**Version:** 1.0
