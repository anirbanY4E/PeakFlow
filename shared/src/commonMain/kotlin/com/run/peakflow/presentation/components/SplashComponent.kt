package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.usecases.GetUserMembershipsUseCase
import com.run.peakflow.domain.usecases.IsUserLoggedInUseCase
import com.run.peakflow.presentation.state.NavigationTarget
import com.run.peakflow.presentation.state.SplashState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        checkAuthState()
    }

    private fun checkAuthState() {
        scope.launch {
            delay(1500) // Show splash for at least 1.5 seconds

            if (!isUserLoggedIn()) {
                _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.WELCOME) }
                onNavigateToWelcome()
                return@launch
            }

            val user = getCurrentUser()
            if (user == null) {
                _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.WELCOME) }
                onNavigateToWelcome()
                return@launch
            }

            // Check if profile is complete
            if (user.name.isBlank()) {
                // Check if user has a community first
                val memberships = getUserMemberships()
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
            val memberships = getUserMemberships()
            if (memberships.isEmpty()) {
                _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.INVITE_CODE) }
                onNavigateToInviteCode()
                return@launch
            }

            // All good, go to main
            _state.update { it.copy(isLoading = false, navigationTarget = NavigationTarget.MAIN) }
            onNavigateToMain()
        }
    }
}