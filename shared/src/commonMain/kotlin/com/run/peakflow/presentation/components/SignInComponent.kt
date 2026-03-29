package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.network.ApiService
import com.run.peakflow.data.network.AuthSessionStatus
import com.run.peakflow.data.repository.UserRepository
import com.run.peakflow.domain.usecases.GetUserMembershipsUseCase
import com.run.peakflow.domain.usecases.SignInUseCase
import com.run.peakflow.domain.usecases.SignInWithGoogleUseCase
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.validation.AuthValidation
import com.run.peakflow.presentation.state.SignInState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SignInComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToSignUp: () -> Unit,
    private val onNavigateToMain: () -> Unit,
    private val onNavigateToInviteCode: () -> Unit,
    private val onNavigateToProfileSetup: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val signIn: SignInUseCase by inject()
    private val signInWithGoogle: SignInWithGoogleUseCase by inject()
    private val getUserMemberships: GetUserMembershipsUseCase by inject()
    private val getCurrentUser: GetCurrentUserUseCase by inject()
    private val apiService: ApiService by inject()
    private val userRepository: UserRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    private var sessionObserverJob: Job? = null

    init {
        lifecycle.doOnDestroy {
            sessionObserverJob?.cancel()
            scope.cancel()
        }
    }

    fun onEmailOrPhoneChanged(value: String) {
        _state.update { it.copy(emailOrPhone = value, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSignInClick() {
        val currentState = _state.value

        // Enhanced validation
        val (isValid, errorMessage) = AuthValidation.isEmailOrPhone(currentState.emailOrPhone)
        if (!isValid) {
            _state.update { it.copy(error = errorMessage ?: "Invalid email or phone") }
            return
        }

        if (currentState.password.isBlank()) {
            _state.update { it.copy(error = "Password is required") }
            return
        }

        if (currentState.password.length < 8) {
            _state.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = signIn(currentState.emailOrPhone.trim(), currentState.password)

            result.onSuccess { user ->
                _state.update { it.copy(isLoading = false, isSuccess = true) }

                // Navigate based on user state
                navigateBasedOnUserState(user.name.isNotBlank())
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    private suspend fun navigateBasedOnUserState(hasCompletedProfile: Boolean) {
        val memberships = getUserMemberships()
        when {
            !hasCompletedProfile -> {
                if (memberships.isEmpty()) {
                    onNavigateToInviteCode()
                } else {
                    onNavigateToProfileSetup()
                }
            }

            memberships.isEmpty() -> onNavigateToInviteCode()
            else -> onNavigateToMain()
        }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onSignUpClick() {
        onNavigateToSignUp()
    }

    fun onForgotPasswordClick() {
        // TODO: Implement forgot password
    }

    fun onGoogleSignInClick() {
        scope.launch {
            _state.update { it.copy(isGoogleLoading = true, error = null) }

            val result = signInWithGoogle()

            result.onSuccess { user ->
                _state.update { it.copy(isGoogleLoading = false, isSuccess = true) }

                // Navigate based on user state (same logic as regular sign-in)
                navigateBasedOnUserState(user.name.isNotBlank())
            }.onFailure { error ->
                // Handle cancelled or browser sign-in gracefully
                when (error.message) {
                    "Sign-in cancelled" -> {
                        _state.update { it.copy(isGoogleLoading = false, error = null) }
                    }
                    "BROWSER_FLOW" -> {
                        // Browser flow: keep loading state and observe session for callback
                        println("SignInComponent: Browser flow started, observing session status...")
                        startObservingSession()
                    }
                    else -> {
                        _state.update { it.copy(isGoogleLoading = false, error = error.message) }
                    }
                }
            }
        }
    }

    /**
     * After browser OAuth flow is triggered, observe the Supabase session status.
     * When the deep link callback exchanges the code for a session, the status
     * will change to Authenticated, and we can navigate forward.
     */
    private fun startObservingSession() {
        sessionObserverJob?.cancel()
        sessionObserverJob = scope.launch {
            apiService.observeSessionStatus().collect { status ->
                when (status) {
                    is AuthSessionStatus.Authenticated -> {
                        println("SignInComponent: Session authenticated via browser flow! userId=${status.userId}")
                        _state.update { it.copy(isGoogleLoading = false, isSuccess = true) }
                        
                        // CRITICAL: Set the userId in UserRepository since the browser
                        // flow bypasses AuthRepository.signInWithGoogle() which normally
                        // does this. Without it, getCurrentUser() and getUserMemberships()
                        // return null/empty because they rely on userRepository.currentUserId.
                        userRepository.setCurrentUserId(status.userId)
                        
                        // Look up user and navigate
                        val user = getCurrentUser()
                        if (user != null) {
                            navigateBasedOnUserState(user.name.isNotBlank())
                        } else {
                            // User is authenticated but profile doesn't exist yet
                            onNavigateToInviteCode()
                        }
                        
                        // Stop observing after navigation
                        sessionObserverJob?.cancel()
                    }
                    is AuthSessionStatus.NotAuthenticated -> {
                        // Still waiting for callback, or user cancelled
                        println("SignInComponent: Session not yet authenticated")
                    }
                    is AuthSessionStatus.Loading -> {
                        // Session is initializing
                    }
                    is AuthSessionStatus.NetworkError -> {
                        _state.update { it.copy(isGoogleLoading = false, error = "Network error during sign-in") }
                        sessionObserverJob?.cancel()
                    }
                }
            }
        }
    }
}