package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthState(
    val isLoggedIn: Boolean = false,
    val isVerified: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val hasCommunity: Boolean = false,
    val currentUserId: String? = null
)