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
                // Wait for Supabase to initialize session from storage with timeout
                if (SupabaseConfig.client.auth.sessionStatus.value is SessionStatus.Initializing) {
                    println("SplashComponent: Waiting for session initialization...")
                    withTimeoutOrNull(5000) {
                        SupabaseConfig.client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
                    }
                    println("SplashComponent: Session initialization finished or timed out (status: ${SupabaseConfig.client.auth.sessionStatus.value})")
                }
            } catch (e: Exception) {
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

                // All good, go to main
                _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.MAIN) }
                onNavigateToMain()
            } catch (e: Exception) {
                println("Failed to initialize session: ${e.message}")
                _state.update { it.copy(isLoading = false, errorMessage = "Failed to connect to servers. Please try again.") }
            }
        }
    }

    fun onRetry() {
        checkAuthState()
    }
}