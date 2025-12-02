package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.CreateUserUseCase
import com.run.peakflow.presentation.state.OnboardingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingComponent(
    componentContext: ComponentContext,
    private val createUserUseCase: CreateUserUseCase,
    private val onOnboardingComplete: () -> Unit
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, error = null) }
    }

    fun onContinueClicked() {
        val currentState = _state.value

        if (!currentState.isNameValid) {
            _state.update { it.copy(error = "Name must be at least 2 characters") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                createUserUseCase(currentState.name.trim(), "Bangalore")
                _state.update { it.copy(isLoading = false, isComplete = true) }
                onOnboardingComplete()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Something went wrong"
                    )
                }
            }
        }
    }
}