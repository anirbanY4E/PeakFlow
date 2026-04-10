package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.network.SupabaseConfig
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.usecases.GetUserMembershipsUseCase
import com.run.peakflow.domain.usecases.IsUserLoggedInUseCase
import com.run.peakflow.presentation.state.NavigationTarget
import com.run.peakflow.presentation.state.SplashState
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SplashComponent(
    componentContext: ComponentContext,
    private val onNavigateToWelcome: () -> Unit,
    private val onNavigateToInviteCode: () -> Unit,
    private val onNavigateToProfileSetup: () -> Unit,
    private val onNavigateToMain: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val isUserLoggedIn: IsUserLoggedInUseCase by inject()
    private val getCurrentUser: GetCurrentUserUseCase by inject()
    private val getUserMemberships: GetUserMembershipsUseCase by inject()
    private val getFeedPosts: com.run.peakflow.domain.usecases.GetFeedPostsUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        checkAuthState()
    }

    private fun checkAuthState() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            try {
                // Wait for a TERMINAL session state before making any navigation decision.
                // We must ride through ALL transient states:
                //   Initializing   → SDK loading session from storage
                //   RefreshFailure → SDK found expired token and is retrying the refresh
                // Using a 10-second timeout to match waitForSessionLoaded for slow networks.
                val currentStatus = SupabaseConfig.client.auth.sessionStatus.value
                val isTransient = currentStatus is SessionStatus.Initializing ||
                        currentStatus::class.simpleName == "RefreshFailure"
                if (isTransient) {
                    println("SplashComponent: Waiting for session to settle (current: $currentStatus)...")
                    withTimeoutOrNull(10_000) {
                        SupabaseConfig.client.auth.sessionStatus.first { status ->
                            status is SessionStatus.Authenticated ||
                                    status is SessionStatus.NotAuthenticated
                        }
                    }
                    println("SplashComponent: Session settled (status: ${SupabaseConfig.client.auth.sessionStatus.value})")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("SplashComponent: Initialization check failed: ${e.message}")
            }

            try {
                if (!isUserLoggedIn()) {
                    _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.WELCOME) }
                    onNavigateToWelcome()
                    return@launch
                }

                // CRITICAL: Ensure Supabase session is actually valid (not expired)
                // When app resumes from background after a long time, the stored JWT may be expired.
                // Supabase with alwaysAutoRefresh should handle this, but we'll try a manual refresh 
                // if it looks like it might be expired or if the SDK hasn't caught up yet.
                try {
                    val currentSession = SupabaseConfig.client.auth.currentSessionOrNull()
                    if (currentSession != null) {
                        val now = kotlin.time.Clock.System.now()
                        if (currentSession.expiresAt < now + 10.seconds) {
                            println("SplashComponent: Session expired or expiring soon, refreshing...")
                            SupabaseConfig.client.auth.refreshCurrentSession()
                            println("SplashComponent: Session refreshed successfully")
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    println("SplashComponent: Session refresh failed: ${e.message}")
                    // If refresh fails, redirect to login
                    _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.WELCOME) }
                    onNavigateToWelcome()
                    return@launch
                }

                // Parallelize user + memberships fetch (both independent after login check)
                val userDeferred = async { getCurrentUser() }
                val membershipsDeferred = async { getUserMemberships() }

                val user = userDeferred.await()
                val memberships = membershipsDeferred.await()

                if (user == null) {
                    _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.WELCOME) }
                    onNavigateToWelcome()
                    return@launch
                }

                // Check if profile is complete
                if (user.name.isBlank()) {
                    if (memberships.isEmpty()) {
                        _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.INVITE_CODE) }
                        onNavigateToInviteCode()
                    } else {
                        _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.PROFILE_SETUP) }
                        onNavigateToProfileSetup()
                    }
                    return@launch
                }

                // Check if user has at least one community
                if (memberships.isEmpty()) {
                    _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.INVITE_CODE) }
                    onNavigateToInviteCode()
                    return@launch
                }

                try {
                    // Option 2: "Hold the Door" - pre-fetch feed posts before leaving the splash screen
                    // This warms the in-memory cache in PostRepository so FeedComponent loads instantly
                    getFeedPosts(limit = 20, offset = 0)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    println("SplashComponent: Feed pre-fetch failed: ${e.message}, silently continuing...")
                }

                // All good, go to main
                _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.MAIN) }
                onNavigateToMain()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("Failed to initialize session: ${e.message}")
                _state.update { it.copy(isLoading = false, errorMessage = "Failed to connect to servers. Please try again.") }
            }
        }
    }

    fun onRetry() {
        checkAuthState()
    }
}