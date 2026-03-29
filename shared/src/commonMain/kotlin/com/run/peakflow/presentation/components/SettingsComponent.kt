package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.domain.usecases.LogoutUseCase
import com.run.peakflow.presentation.state.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onLogout: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val logoutUseCase: LogoutUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }


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
        scope.launch {
            logoutUseCase()
            onLogout()
        }
    }

    fun onLogoutCancel() {
        _state.update { it.copy(isLogoutConfirmVisible = false) }
    }
}