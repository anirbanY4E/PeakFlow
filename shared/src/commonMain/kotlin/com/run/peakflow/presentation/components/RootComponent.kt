package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.repository.AuthRepository
import com.run.peakflow.presentation.navigation.DeepLinkNavigator
import com.run.peakflow.presentation.state.CommunityTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val authRepository: AuthRepository by inject()

    /**
     * Flag to detect if we just restored the state (not starting from Splash/Welcome).
     * We use this to provide a "grace period" for Supabase to warm up before redirecting.
     */
    private var isRestorationGracePeriod = false

    val childStack: Value<ChildStack<Config, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Splash,
        handleBackButton = true,
        childFactory = ::createChild
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        
        // Detect if we were restored to a protected screen
        val activeConfig = childStack.value.active.configuration
        if (activeConfig !is Config.Splash && activeConfig !is Config.Welcome && 
            activeConfig !is Config.SignIn && activeConfig !is Config.SignUp) {
            println("RootComponent: Detected restoration to protected screen ($activeConfig). Starting grace period...")
            isRestorationGracePeriod = true
            scope.launch {
                kotlinx.coroutines.delay(3000) // 3s grace period for Supabase warm-up
                println("RootComponent: Restoration grace period expired.")
                isRestorationGracePeriod = false
                // Re-trigger auth check manually once grace period ends
                checkAuthAndRedirect(authRepository.authState.value)
            }
        }

        observeDeepLinks()
        observeAuthState()
    }

    private fun observeAuthState() {
        scope.launch {
            authRepository.authState.collect { state ->
                checkAuthAndRedirect(state)
            }
        }
    }

    private fun checkAuthAndRedirect(state: com.run.peakflow.data.models.AuthState) {
        // Don't act while still initializing (e.g., app resuming from background)
        if (state.isInitializing) return

        // During restoration grace period, don't redirect to Welcome even if "not logged in"
        // because Supabase might still be reading from storage.
        if (isRestorationGracePeriod) {
            println("RootComponent: Ignoring 'NotLoggedIn' state during restoration grace period")
            return
        }

        val activeConfig = childStack.value.active.configuration
        if (!state.isLoggedIn && activeConfig !is Config.Splash && 
            activeConfig !is Config.Welcome && activeConfig !is Config.SignIn && 
            activeConfig !is Config.SignUp) {
            println("RootComponent: Definitive 'NotLoggedIn' state detected (not initializing, not in grace period). Redirecting to Welcome...")
            navigation.replaceAll(Config.Welcome)
        }
    }

    private fun observeDeepLinks() {
        scope.launch {
            DeepLinkNavigator.events.collect { deepLink ->
                when (deepLink) {
                    is DeepLinkNavigator.DeepLink.CommunityPost -> {
                        // Push CommunityDetail with Posts tab pre-selected
                        navigation.pushNew(
                            Config.CommunityDetail(
                                communityId = deepLink.communityId,
                                initialTab = CommunityTab.POSTS
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createChild(config: Config, componentContext: ComponentContext): Child {
        return when (config) {
            is Config.Splash -> Child.Splash(
                SplashComponent(
                    componentContext = componentContext,
                    onNavigateToWelcome = { navigation.replaceAll(Config.Welcome) },
                    onNavigateToInviteCode = { navigation.replaceAll(Config.InviteCode) },
                    onNavigateToProfileSetup = { navigation.replaceAll(Config.ProfileSetup) },
                    onNavigateToMain = { navigation.replaceAll(Config.Main) }
                )
            )
            is Config.Welcome -> Child.Welcome(
                WelcomeComponent(
                    componentContext = componentContext,
                    onNavigateToSignUp = { navigation.pushNew(Config.SignUp) },
                    onNavigateToSignIn = { navigation.pushNew(Config.SignIn) }
                )
            )
            is Config.SignUp -> Child.SignUp(
                SignUpComponent(
                    componentContext = componentContext,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToOtp = { userId, sentTo ->
                        navigation.pushNew(Config.OtpVerification(userId, sentTo))
                    },
                    onNavigateToSignIn = {
                        navigation.pop()
                        navigation.pushNew(Config.SignIn)
                    },
                    onNavigateToInviteCode = { navigation.replaceAll(Config.InviteCode) },
                    onNavigateToMain = { navigation.replaceAll(Config.Main) },
                    onNavigateToProfileSetup = { navigation.replaceAll(Config.ProfileSetup) }
                )
            )
            is Config.SignIn -> Child.SignIn(
                SignInComponent(
                    componentContext = componentContext,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToSignUp = {
                        navigation.pop()
                        navigation.pushNew(Config.SignUp)
                    },
                    onNavigateToMain = { navigation.replaceAll(Config.Main) },
                    onNavigateToInviteCode = { navigation.replaceAll(Config.InviteCode) },
                    onNavigateToProfileSetup = { navigation.replaceAll(Config.ProfileSetup) }
                )
            )
            is Config.OtpVerification -> Child.OtpVerification(
                OtpVerificationComponent(
                    componentContext = componentContext,
                    userId = config.userId,
                    sentTo = config.sentTo,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToInviteCode = { navigation.replaceAll(Config.InviteCode) }
                )
            )
            is Config.InviteCode -> Child.InviteCode(
                InviteCodeComponent(
                    componentContext = componentContext,
                    onNavigateToProfileSetup = { navigation.replaceAll(Config.ProfileSetup) }
                )
            )
            is Config.ProfileSetup -> Child.ProfileSetup(
                ProfileSetupComponent(
                    componentContext = componentContext,
                    onNavigateToMain = { navigation.replaceAll(Config.Main) }
                )
            )
            is Config.Main -> Child.Main(
                MainComponent(
                    componentContext = componentContext,
                    onNavigateToCommunityDetail = { communityId ->
                        navigation.pushNew(Config.CommunityDetail(communityId))
                    },
                    onNavigateToEventDetail = { eventId ->
                        navigation.pushNew(Config.EventDetail(eventId))
                    },
                    onNavigateToPostDetail = { postId ->
                        navigation.pushNew(Config.PostDetail(postId))
                    },
                    onNavigateToSettings = { navigation.pushNew(Config.Settings) },
                    onNavigateToEditProfile = { navigation.pushNew(Config.EditProfile) },
                    onLogout = { navigation.replaceAll(Config.Welcome) }
                )
            )
            is Config.CommunityDetail -> Child.CommunityDetail(
                CommunityDetailComponent(
                    componentContext = componentContext,
                    communityId = config.communityId,
                    initialTab = config.initialTab,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToEventDetail = { eventId ->
                        navigation.pushNew(Config.EventDetail(eventId))
                    },
                    onNavigateToPostDetail = { postId ->
                        navigation.pushNew(Config.PostDetail(postId))
                    },
                    onNavigateToGenerateInvite = { communityId ->
                        navigation.pushNew(Config.GenerateInvite(communityId))
                    },
                    onNavigateToJoinRequests = { communityId ->
                        navigation.pushNew(Config.JoinRequests(communityId))
                    },
                    onNavigateToCreateEvent = { communityId ->
                        navigation.pushNew(Config.CreateEvent(communityId))
                    },
                    onNavigateToCreatePost = { communityId ->
                        navigation.pushNew(Config.CreatePost(communityId))
                    }
                )
            )
            is Config.EventDetail -> Child.EventDetail(
                EventDetailComponent(
                    componentContext = componentContext,
                    eventId = config.eventId,
                    onNavigateBack = { navigation.pop() }
                )
            )
            is Config.PostDetail -> Child.PostDetail(
                PostDetailComponent(
                    componentContext = componentContext,
                    postId = config.postId,
                    onNavigateBack = { navigation.pop() }
                )
            )
            is Config.GenerateInvite -> Child.GenerateInvite(
                GenerateInviteComponent(
                    componentContext = componentContext,
                    communityId = config.communityId,
                    onNavigateBack = { navigation.pop() }
                )
            )
            is Config.JoinRequests -> Child.JoinRequests(
                JoinRequestsComponent(
                    componentContext = componentContext,
                    communityId = config.communityId,
                    onNavigateBack = { navigation.pop() }
                )
            )
            is Config.Settings -> Child.Settings(
                SettingsComponent(
                    componentContext = componentContext,
                    onNavigateBack = { navigation.pop() },
                    onLogout = { navigation.replaceAll(Config.Welcome) }
                )
            )
            is Config.CreateEvent -> Child.CreateEvent(
                CreateEventComponent(
                    componentContext = componentContext,
                    communityId = config.communityId,
                    onNavigateBack = { navigation.pop() },
                    onEventCreated = { navigation.pop() }
                )
            )
            is Config.CreatePost -> Child.CreatePost(
                CreatePostComponent(
                    componentContext = componentContext,
                    communityId = config.communityId,
                    onNavigateBack = { navigation.pop() },
                    onPostCreated = { navigation.pop() }
                )
            )
            is Config.EditProfile -> Child.EditProfile(
                EditProfileComponent(
                    componentContext = componentContext,
                    onNavigateBack = { navigation.pop() }
                )
            )
        }
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object Splash : Config()

        @Serializable
        data object Welcome : Config()

        @Serializable
        data object SignUp : Config()

        @Serializable
        data object SignIn : Config()

        @Serializable
        data class OtpVerification(val userId: String, val sentTo: String) : Config()

        @Serializable
        data object InviteCode : Config()

        @Serializable
        data object ProfileSetup : Config()

        @Serializable
        data object Main : Config()

        @Serializable
        data class CommunityDetail(
            val communityId: String,
            val initialTab: CommunityTab = CommunityTab.POSTS
        ) : Config()

        @Serializable
        data class EventDetail(val eventId: String) : Config()

        @Serializable
        data class PostDetail(val postId: String) : Config()

        @Serializable
        data class GenerateInvite(val communityId: String) : Config()

        @Serializable
        data class JoinRequests(val communityId: String) : Config()

        @Serializable
        data object Settings : Config()

        @Serializable
        data class CreateEvent(val communityId: String) : Config()

        @Serializable
        data class CreatePost(val communityId: String) : Config()

        @Serializable
        data object EditProfile : Config()
    }

    sealed class Child {
        data class Splash(val component: SplashComponent) : Child()
        data class Welcome(val component: WelcomeComponent) : Child()
        data class SignUp(val component: SignUpComponent) : Child()
        data class SignIn(val component: SignInComponent) : Child()
        data class OtpVerification(val component: OtpVerificationComponent) : Child()
        data class InviteCode(val component: InviteCodeComponent) : Child()
        data class ProfileSetup(val component: ProfileSetupComponent) : Child()
        data class Main(val component: MainComponent) : Child()
        data class CommunityDetail(val component: CommunityDetailComponent) : Child()
        data class EventDetail(val component: EventDetailComponent) : Child()
        data class PostDetail(val component: PostDetailComponent) : Child()
        data class GenerateInvite(val component: GenerateInviteComponent) : Child()
        data class JoinRequests(val component: JoinRequestsComponent) : Child()
        data class Settings(val component: SettingsComponent) : Child()
        data class CreateEvent(val component: CreateEventComponent) : Child()
        data class CreatePost(val component: CreatePostComponent) : Child()
        data class EditProfile(val component: EditProfileComponent) : Child()
    }
}