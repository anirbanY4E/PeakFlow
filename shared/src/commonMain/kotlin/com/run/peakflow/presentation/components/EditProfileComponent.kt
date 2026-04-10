package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.network.AuthenticationException
import com.run.peakflow.data.repository.AuthRepository
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.usecases.UpdateUserUseCase
import com.run.peakflow.presentation.state.EditProfileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EditProfileComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getCurrentUser: GetCurrentUserUseCase by inject()
    private val updateUser: UpdateUserUseCase by inject()
    private val authRepository: AuthRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val user = getCurrentUser()
                if (user != null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            name = user.name,
                            city = user.city,
                            interests = user.interests,
                            currentAvatarUrl = user.avatarUrl
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "User not found") }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, error = null) }
    }

    fun onCityChanged(city: String) {
        _state.update { it.copy(city = city, error = null) }
    }

    fun onInterestToggled(category: EventCategory) {
        _state.update { currentState ->
            val newInterests = if (category in currentState.interests) {
                currentState.interests - category
            } else {
                currentState.interests + category
            }
            currentState.copy(interests = newInterests, error = null)
        }
    }

    fun onAvatarChanged(avatarBytes: ByteArray?) {
        _state.update { it.copy(avatarBytes = avatarBytes) }
    }

    fun onSaveClick() {
        val currentState = _state.value

        if (currentState.name.isBlank() || currentState.name.length < 2) {
            _state.update { it.copy(error = "Name must be at least 2 characters") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentUser = getCurrentUser()
            if (currentUser == null) {
                _state.update { it.copy(isLoading = false, error = "User not found") }
                return@launch
            }

            val updatedUser = currentUser.copy(
                name = currentState.name,
                city = currentState.city,
                interests = currentState.interests
            )

            val result = updateUser(updatedUser, currentState.avatarBytes)

            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
                onNavigateBack()
            }.onFailure { error ->
                if (error is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun onBackClick() {
        onNavigateBack()
    }
}