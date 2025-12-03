package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.JoinCommunityViaInviteUseCase
import com.run.peakflow.domain.usecases.ValidateInviteCodeUseCase
import com.run.peakflow.presentation.state.InviteCodeState
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

class InviteCodeComponent(
    componentContext: ComponentContext,
    private val onNavigateToProfileSetup: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val validateInviteCode: ValidateInviteCodeUseCase by inject()
    private val joinCommunityViaInvite: JoinCommunityViaInviteUseCase by inject()
    private val getCommunityById: GetCommunityById by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(InviteCodeState())
    val state: StateFlow<InviteCodeState> = _state.asStateFlow()

    fun onCodeChanged(code: String) {
        val formattedCode = code.uppercase().filter { it.isLetterOrDigit() || it == '-' }
        _state.update { it.copy(code = formattedCode, error = null, validatedCommunity = null) }

        // Auto-validate when code looks complete
        if (formattedCode.length >= 10) {
            validateCode()
        }
    }

    private fun validateCode() {
        val currentCode = _state.value.code
        if (currentCode.isBlank()) return

        scope.launch {
            _state.update { it.copy(isValidating = true, error = null) }

            val result = validateInviteCode(currentCode)

            result.onSuccess { invite ->
                val community = getCommunityById(invite.communityId)
                _state.update {
                    it.copy(
                        isValidating = false,
                        validatedCommunity = community
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isValidating = false,
                        error = error.message,
                        validatedCommunity = null
                    )
                }
            }
        }
    }

    fun onJoinClick() {
        val currentState = _state.value

        if (currentState.code.isBlank()) {
            _state.update { it.copy(error = "Please enter an invite code") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = joinCommunityViaInvite(currentState.code)

            result.onSuccess { membership ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        joinedCommunityId = membership.communityId
                    )
                }
                onNavigateToProfileSetup()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}