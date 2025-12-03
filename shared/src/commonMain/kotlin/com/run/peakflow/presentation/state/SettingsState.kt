package com.run.peakflow.presentation.state

data class SettingsState(
    val pushNotificationsEnabled: Boolean = true,
    val emailUpdatesEnabled: Boolean = true,
    val whatsappRemindersEnabled: Boolean = false,
    val darkModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLogoutConfirmVisible: Boolean = false
)