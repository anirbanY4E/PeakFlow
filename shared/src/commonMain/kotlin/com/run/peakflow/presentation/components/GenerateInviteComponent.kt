package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.GenerateInviteCodeUseCase
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.GetUserInviteCodesUseCase
import com.run.peakflow.presentation.state.GenerateInviteState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GenerateInviteComponent(
    componentContext: ComponentContext,
    private val communityId: String,
    private val onNavigateBack: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getCommunityById: GetCommunityById by inject()
    private val generateInviteCode: GenerateInviteCodeUseCase by inject()
    private val getUserInviteCodes: GetUserInviteCodesUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(GenerateInviteState())
    val state: StateFlow<GenerateInviteState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val community = getCommunityById(communityId)
                val existingCodes = getUserInviteCodes(communityId)

                _state.update {
                    it.copy(
                        isLoading = false,
                        community = community,
                        existingCodes = existingCodes
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onGenerateClick() {
        scope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }

            val currentState = _state.value
            val result = generateInviteCode(
                communityId = communityId,
                maxUses = currentState.maxUses,
                expiresInDays = currentState.expiresInDays
            )

            result.onSuccess { invite ->
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generatedCode = invite,
                        existingCodes = listOf(invite) + it.existingCodes
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isGenerating = false, error = error.message) }
            }
        }
    }

    fun onExpiryDaysChanged(days: Int) {
        _state.update { it.copy(expiresInDays = days) }
    }

    fun onMaxUsesChanged(maxUses: Int?) {
        _state.update { it.copy(maxUses = maxUses) }
    }

    fun onCopyCode() {
        _state.update { it.copy(isCopied = true) }
        scope.launch {
            delay(2000)
            _state.update { it.copy(isCopied = false) }
        }
    }
}