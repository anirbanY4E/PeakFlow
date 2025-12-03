package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.ApproveJoinRequestUseCase
import com.run.peakflow.domain.usecases.GetPendingJoinRequestsUseCase
import com.run.peakflow.domain.usecases.GetUserByIdUseCase
import com.run.peakflow.domain.usecases.RejectJoinRequestUseCase
import com.run.peakflow.presentation.state.JoinRequestWithUser
import com.run.peakflow.presentation.state.JoinRequestsState
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

class JoinRequestsComponent(
    componentContext: ComponentContext,
    private val communityId: String,
    private val onNavigateBack: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getPendingJoinRequests: GetPendingJoinRequestsUseCase by inject()
    private val approveJoinRequest: ApproveJoinRequestUseCase by inject()
    private val rejectJoinRequest: RejectJoinRequestUseCase by inject()
    private val getUserById: GetUserByIdUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(JoinRequestsState())
    val state: StateFlow<JoinRequestsState> = _state.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val requests = getPendingJoinRequests(communityId)
                val requestsWithUsers = requests.map { request ->
                    val user = getUserById(request.userId)
                    JoinRequestWithUser(request = request, user = user)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        pendingRequests = requestsWithUsers
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onRefresh() {
        scope.launch {
            _state.update { it.copy(isRefreshing = true) }
            loadRequests()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onApproveClick(requestId: String) {
        scope.launch {
            _state.update { it.copy(processingRequestIds = it.processingRequestIds + requestId) }

            val result = approveJoinRequest(requestId)

            result.onSuccess {
                _state.update { currentState ->
                    val approvedRequest = currentState.pendingRequests.find { it.request.id == requestId }
                    currentState.copy(
                        processingRequestIds = currentState.processingRequestIds - requestId,
                        pendingRequests = currentState.pendingRequests.filter { it.request.id != requestId },
                        recentlyApproved = approvedRequest?.let { listOf(it) + currentState.recentlyApproved }
                            ?: currentState.recentlyApproved
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        processingRequestIds = it.processingRequestIds - requestId,
                        error = error.message
                    )
                }
            }
        }
    }

    fun onRejectClick(requestId: String) {
        scope.launch {
            _state.update { it.copy(processingRequestIds = it.processingRequestIds + requestId) }

            val result = rejectJoinRequest(requestId)

            result.onSuccess {
                _state.update { currentState ->
                    currentState.copy(
                        processingRequestIds = currentState.processingRequestIds - requestId,
                        pendingRequests = currentState.pendingRequests.filter { it.request.id != requestId }
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        processingRequestIds = it.processingRequestIds - requestId,
                        error = error.message
                    )
                }
            }
        }
    }
}