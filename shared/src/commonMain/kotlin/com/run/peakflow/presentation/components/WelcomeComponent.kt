package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.presentation.state.WelcomeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WelcomeComponent(
    componentContext: ComponentContext,
    private val onNavigateToSignUp: () -> Unit,
    private val onNavigateToSignIn: () -> Unit
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(WelcomeState())
    val state: StateFlow<WelcomeState> = _state.asStateFlow()

    fun onPageChanged(page: Int) {
        _state.update { it.copy(currentPage = page) }
    }

    fun onGetStartedClick() {
        onNavigateToSignUp()
    }

    fun onSignInClick() {
        onNavigateToSignIn()
    }
}