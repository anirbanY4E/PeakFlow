package com.run.peakflow.presentation.state

data class SplashState(
    val isLoading: Boolean = true,
    val navigationTarget: NavigationTarget = NavigationTarget.NONE
)

enum class NavigationTarget {
    NONE,
    WELCOME,
    INVITE_CODE,
    PROFILE_SETUP,
    MAIN
}