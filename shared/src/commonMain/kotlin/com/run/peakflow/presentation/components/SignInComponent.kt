package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.GetUserMembershipsUseCase
import com.run.peakflow.domain.usecases.SignInUseCase
import com.run.peakflow.presentation.state.SignInState
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

class SignInComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToSignUp: () -> Unit,
    private val onNavigateToMain: () -> Unit,
    private val onNavigateToInviteCode: () -> Unit,
    private val onNavigateToProfileSetup: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val signIn: SignInUseCase by inject()
    private val getUserMemberships: GetUserMembershipsUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

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

        if (currentState.emailOrPhone.isBlank()) {
            _state.update { it.copy(error = "Email or phone is required") }
            return
        }
        if (currentState.password.isBlank()) {
            _state.update { it.copy(error = "Password is required") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = signIn(currentState.emailOrPhone, currentState.password)

            result.onSuccess { user ->
                _state.update { it.copy(isLoading = false, isSuccess = true) }

                // Determine where to navigate
                if (user.name.isBlank()) {
                    val memberships = getUserMemberships()
                    if (memberships.isEmpty()) {
                        onNavigateToInviteCode()
                    } else {
                        onNavigateToProfileSetup()
                    }
                } else {
                    val memberships = getUserMemberships()
                    if (memberships.isEmpty()) {
                        onNavigateToInviteCode()
                    } else {
                        onNavigateToMain()
                    }
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
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
}