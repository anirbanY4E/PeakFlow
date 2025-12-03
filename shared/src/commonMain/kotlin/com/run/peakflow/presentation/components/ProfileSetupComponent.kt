package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.domain.usecases.CompleteProfileUseCase
import com.run.peakflow.presentation.state.ProfileSetupState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProfileSetupComponent(
    componentContext: ComponentContext,
    private val onNavigateToMain: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val completeProfile: CompleteProfileUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state.asStateFlow()

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, error = null) }
    }

    fun onCityChanged(city: String) {
        _state.update { it.copy(city = city, error = null) }
    }

    fun onInterestToggled(category: EventCategory) {
        _state.update { currentState ->
            val newInterests = if (category in currentState.selectedInterests) {
                currentState.selectedInterests - category
            } else {
                currentState.selectedInterests + category
            }
            currentState.copy(selectedInterests = newInterests, error = null)
        }
    }

    fun onAvatarChanged(avatarUrl: String?) {
        _state.update { it.copy(avatarUrl = avatarUrl) }
    }

    fun onCompleteClick() {
        val currentState = _state.value

        if (currentState.name.isBlank() || currentState.name.length < 2) {
            _state.update { it.copy(error = "Name must be at least 2 characters") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = completeProfile(
                name = currentState.name,
                city = currentState.city,
                interests = currentState.selectedInterests,
                avatarUrl = currentState.avatarUrl
            )

            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
                onNavigateToMain()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}