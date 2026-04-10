package com.run.peakflow.data.repository

import com.run.peakflow.data.models.AuthState
import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.ApiService
import com.run.peakflow.data.network.AuthSessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepository(
    private val api: ApiService,
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private val settlementMutex = Mutex()

    init {
        observeSessionStatus()
    }

    private fun observeSessionStatus() {
        scope.launch {
            api.observeSessionStatus().collect { status ->
                println("AuthRepository: Session status update: $status")
                when (status) {
                    is AuthSessionStatus.Authenticated -> {
                        val userId = status.userId
                        userRepository.setCurrentUserId(userId)
                        // If we don't have user info yet, fetch it to update boarding status
                        try {
                            val user = userRepository.getUser(userId)
                            // Hardened profile check: only update if we got a user back
                            // If user is null (fetch failure), we don't change hasCompletedOnboarding
                            // to avoid resetting it to 'false' incorrectly.
                            _authState.value = _authState.value.copy(
                                isLoggedIn = true,
                                isInitializing = false,
                                isVerified = user?.isVerified ?: _authState.value.isVerified,
                                hasCompletedOnboarding = if (user != null) user.name.isNotBlank() else _authState.value.hasCompletedOnboarding,
                                currentUserId = userId
                            )
                        } catch (e: Exception) {
                            println("AuthRepository: Failed to fetch profile on session restoration: ${e.message}")
                            // Keep isLoggedIn=true but keep previous profile state if available
                            _authState.value = _authState.value.copy(
                                isLoggedIn = true,
                                isInitializing = false,
                                currentUserId = userId
                            )
                        }
                    }
                    AuthSessionStatus.NotAuthenticated -> {
                        // Guard: always confirm NotAuthenticated is truly terminal before acting.
                        // The SDK can briefly emit NotAuthenticated during RefreshFailure retries
                        // even AFTER the app was previously authenticated. We must not log the
                        // user out based on a transient signal.
                        if (_authState.value.isInitializing) {
                            println("AuthRepository: Received NotAuthenticated while initializing, waiting for settlement...")
                        } else {
                            println("AuthRepository: Received NotAuthenticated (post-init), confirming with waitForAuthenticated...")
                        }

                        val settledUserId = settlementMutex.withLock {
                            // If another coroutine already resolved this and transitioned us to
                            // Authenticated, skip the wait.
                            if (!_authState.value.isInitializing && _authState.value.isLoggedIn) {
                                // We were already confirmed authenticated — this is a stale
                                // NotAuthenticated event from a RefreshFailure cycle we survived.
                                println("AuthRepository: Already authenticated, ignoring stale NotAuthenticated")
                                return@collect
                            }
                            // Use waitForAuthenticated (NOT waitForSessionLoaded) so we wait up to
                            // 10s for the SDK to complete its refresh retry.
                            // waitForSessionLoaded() would return null immediately because the
                            // current StateFlow value IS NotAuthenticated (a terminal state for it),
                            // defeating the entire purpose of this guard.
                            api.waitForAuthenticated()
                        }

                        if (settledUserId != null) {
                            println("AuthRepository: Settled on Authenticated ($settledUserId), ignoring NotAuthenticated")
                            return@collect
                        }
                        println("AuthRepository: Confirmed NotAuthenticated — clearing session")

                        userRepository.setCurrentUserId(null)
                        _authState.value = AuthState(isLoggedIn = false, isInitializing = false)
                    }
                    AuthSessionStatus.Loading -> {
                        _authState.value = _authState.value.copy(isInitializing = true)
                    }
                    AuthSessionStatus.NetworkError -> {
                        /* Keep current state but stop initializing if we have a session */
                        if (_authState.value.isLoggedIn) {
                            _authState.value = _authState.value.copy(isInitializing = false)
                        }
                    }
                }
            }
        }
    }

    suspend fun signUp(
        email: String?,
        phone: String?,
        password: String
    ): User {
        val user = api.signUp(email, phone, password)
        userRepository.setCurrentUserId(user.id)
        _authState.value = _authState.value.copy(
            isLoggedIn = true,
            isVerified = false,
            currentUserId = user.id
        )
        return user
    }

    suspend fun signIn(
        emailOrPhone: String,
        password: String
    ): User? {
        val user = api.signIn(emailOrPhone, password)
        if (user != null) {
            userRepository.setCurrentUserId(user.id)
            _authState.value = _authState.value.copy(
                isLoggedIn = true,
                isVerified = user.isVerified,
                hasCompletedOnboarding = user.name.isNotBlank(),
                currentUserId = user.id
            )
        }
        return user
    }

    suspend fun signInWithGoogle(
        idToken: String,
        email: String,
        displayName: String?,
        photoUrl: String?
    ): User {
        val user = api.signInWithGoogle(idToken, email, displayName, photoUrl)
        userRepository.setCurrentUserId(user.id)
        _authState.value = _authState.value.copy(
            isLoggedIn = true,
            isVerified = true, // Google users are pre-verified
            hasCompletedOnboarding = user.name.isNotBlank(),
            currentUserId = user.id
        )
        return user
    }

    suspend fun verifyOtp(userId: String, otp: String): Boolean {
        val success = api.verifyOtp(userId, otp)
        if (success) {
            _authState.value = _authState.value.copy(isVerified = true)
        }
        return success
    }

    suspend fun resendOtp(userId: String): Boolean {
        return api.resendOtp(userId)
    }

    fun updateAuthState(
        hasCommunity: Boolean? = null,
        hasCompletedOnboarding: Boolean? = null
    ) {
        _authState.value = _authState.value.copy(
            hasCommunity = hasCommunity ?: _authState.value.hasCommunity,
            hasCompletedOnboarding = hasCompletedOnboarding ?: _authState.value.hasCompletedOnboarding
        )
    }

    /**
     * Called when a data-layer operation fails due to definitive authentication loss.
     * This triggers a global logout state and redirects the user to the Welcome screen.
     */
    fun handleAuthenticationError() {
        scope.launch {
            logout()
        }
    }

    suspend fun logout() {
        userRepository.logout()
        _authState.value = AuthState(isLoggedIn = false, isInitializing = false)
    }

    suspend fun getCurrentUserId(): String? = userRepository.getCurrentUserId()
}