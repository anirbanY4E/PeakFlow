package com.run.peakflow.data.repository

import com.run.peakflow.data.models.AuthState
import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val api: ApiService,
    private val userRepository: UserRepository
) {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

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

    fun logout() {
        userRepository.logout()
        _authState.value = AuthState()
    }

    fun getCurrentUserId(): String? = userRepository.getCurrentUserId()
}