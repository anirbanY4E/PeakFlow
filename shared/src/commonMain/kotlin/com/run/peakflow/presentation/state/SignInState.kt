package com.run.peakflow.presentation.state

data class SignInState(
    val emailOrPhone: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)