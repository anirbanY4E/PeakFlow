package com.run.peakflow.presentation.state

data class OtpVerificationState(
    val otp: String = "",
    val userId: String = "",
    val sentTo: String = "",
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val resendCountdown: Int = 0
)