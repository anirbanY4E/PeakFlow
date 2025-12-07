package com.run.peakflow.presentation.state

data class SignUpState(
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val userId: String? = null
)