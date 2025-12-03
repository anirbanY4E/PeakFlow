package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.ResendOtpUseCase
import com.run.peakflow.domain.usecases.VerifyOtpUseCase
import com.run.peakflow.presentation.state.OtpVerificationState
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

class OtpVerificationComponent(
    componentContext: ComponentContext,
    private val userId: String,
    private val sentTo: String,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToInviteCode: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val verifyOtp: VerifyOtpUseCase by inject()
    private val resendOtp: ResendOtpUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(
        OtpVerificationState(
            userId = userId,
            sentTo = sentTo,
            resendCountdown = 45
        )
    )
    val state: StateFlow<OtpVerificationState> = _state.asStateFlow()

    init {
        startCountdown()
    }

    private fun startCountdown() {
        scope.launch {
            while (_state.value.resendCountdown > 0) {
                delay(1000)
                _state.update { it.copy(resendCountdown = it.resendCountdown - 1) }
            }
        }
    }

    fun onOtpChanged(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _state.update { it.copy(otp = otp, error = null) }

            // Auto-verify when 6 digits entered
            if (otp.length == 6) {
                onVerifyClick()
            }
        }
    }

    fun onVerifyClick() {
        val currentState = _state.value

        if (currentState.otp.length != 6) {
            _state.update { it.copy(error = "Please enter 6-digit OTP") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = verifyOtp(userId, currentState.otp)

            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
                onNavigateToInviteCode()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message, otp = "") }
            }
        }
    }

    fun onResendClick() {
        if (_state.value.resendCountdown > 0) return

        scope.launch {
            _state.update { it.copy(isResending = true, error = null) }

            val result = resendOtp(userId)

            result.onSuccess {
                _state.update { it.copy(isResending = false, resendCountdown = 45) }
                startCountdown()
            }.onFailure { error ->
                _state.update { it.copy(isResending = false, error = error.message) }
            }
        }
    }

    fun onBackClick() {
        onNavigateBack()
    }
}