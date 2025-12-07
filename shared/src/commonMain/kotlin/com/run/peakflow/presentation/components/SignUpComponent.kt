package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.GetUserMembershipsUseCase
import com.run.peakflow.domain.usecases.SignInWithGoogleUseCase
import com.run.peakflow.domain.usecases.SignUpUseCase
import com.run.peakflow.domain.validation.AuthValidation
import com.run.peakflow.presentation.state.SignUpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SignUpComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToOtp: (userId: String, sentTo: String) -> Unit,
    private val onNavigateToSignIn: () -> Unit,
    private val onNavigateToInviteCode: () -> Unit = {}
) : ComponentContext by componentContext, KoinComponent {

    private val signUp: SignUpUseCase by inject()
    private val signInWithGoogle: SignInWithGoogleUseCase by inject()
    private val getUserMemberships: GetUserMembershipsUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, error = null) }
    }

    fun onPhoneChanged(phone: String) {
        _state.update { it.copy(phone = phone, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update { it.copy(confirmPassword = confirmPassword, error = null) }
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onSignUpClick() {
        val currentState = _state.value

        // Enhanced Validation
        if (currentState.email.isBlank() && currentState.phone.isBlank()) {
            _state.update { it.copy(error = "Email or phone is required") }
            return
        }

        // Validate email if provided
        if (currentState.email.isNotBlank() && !AuthValidation.isValidEmail(currentState.email)) {
            _state.update { it.copy(error = "Invalid email format") }
            return
        }

        // Validate phone if provided
        if (currentState.phone.isNotBlank() && !AuthValidation.isValidPhone(currentState.phone)) {
            _state.update { it.copy(error = "Invalid phone format (use 10-15 digits)") }
            return
        }

        // Validate password strength
        val passwordError = AuthValidation.getPasswordStrengthMessage(currentState.password)
        if (passwordError != null) {
            _state.update { it.copy(error = passwordError) }
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = signUp(
                email = currentState.email.ifBlank { null },
                phone = currentState.phone.ifBlank { null },
                password = currentState.password
            )

            result.onSuccess { user ->
                _state.update { it.copy(isLoading = false, isSuccess = true, userId = user.id) }
                val sentTo = currentState.email.ifBlank { currentState.phone }
                onNavigateToOtp(user.id, sentTo)
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onSignInClick() {
        onNavigateToSignIn()
    }

    fun onGoogleSignUpClick() {
        scope.launch {
            _state.update { it.copy(isGoogleLoading = true, error = null) }

            val result = signInWithGoogle()

            result.onSuccess { user ->
                _state.update { it.copy(isGoogleLoading = false, isSuccess = true, userId = user.id) }

                // Google users skip OTP verification
                // Navigate based on profile completion
                val memberships = getUserMemberships()
                if (memberships.isEmpty()) {
                    onNavigateToInviteCode()
                }
            }.onFailure { error ->
                _state.update { it.copy(isGoogleLoading = false, error = error.message) }
            }
        }
    }
}