package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.LogoutUseCase
import com.run.peakflow.presentation.state.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onLogout: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val logoutUseCase: LogoutUseCase by inject()

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun onBackClick() {
        onNavigateBack()
    }

    fun onPushNotificationsToggle() {
        _state.update { it.copy(pushNotificationsEnabled = !it.pushNotificationsEnabled) }
    }

    fun onEmailUpdatesToggle() {
        _state.update { it.copy(emailUpdatesEnabled = !it.emailUpdatesEnabled) }
    }

    fun onWhatsappRemindersToggle() {
        _state.update { it.copy(whatsappRemindersEnabled = !it.whatsappRemindersEnabled) }
    }

    fun onDarkModeToggle() {
        _state.update { it.copy(darkModeEnabled = !it.darkModeEnabled) }
    }

    fun onLogoutClick() {
        _state.update { it.copy(isLogoutConfirmVisible = true) }
    }

    fun onLogoutConfirm() {
        logoutUseCase()
        onLogout()
    }

    fun onLogoutCancel() {
        _state.update { it.copy(isLogoutConfirmVisible = false) }
    }
}