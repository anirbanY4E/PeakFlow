package com.run.peakflow.presentation.state

data class OnboardingState(
    val name: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
) {
    val isNameValid: Boolean get() = name.trim().length >= 2
}