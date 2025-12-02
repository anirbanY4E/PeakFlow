package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.CheckInToEvent
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.usecases.GetEventById
import com.run.peakflow.domain.usecases.GetEventCheckInStatus
import com.run.peakflow.domain.usecases.GetEventRsvpStatus
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.presentation.state.EventDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventDetailComponent(
    componentContext: ComponentContext,
    private val eventId: String,
    private val getEventById: GetEventById,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getEventRsvpStatus: GetEventRsvpStatus,
    private val getEventCheckInStatus: GetEventCheckInStatus,
    private val rsvpToEvent: RsvpToEvent,
    private val checkInToEvent: CheckInToEvent,
    private val onBack: () -> Unit
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<EventDetailState>(EventDetailState.Loading)
    val state: StateFlow<EventDetailState> = _state.asStateFlow()

    init {
        loadEventDetails()
    }

    fun loadEventDetails() {
        scope.launch {
            _state.value = EventDetailState.Loading

            try {
                val event = getEventById(eventId)

                if (event == null) {
                    _state.value = EventDetailState.Error("Event not found")
                    return@launch
                }

                val user = getCurrentUser()
                val userId = user?.id ?: ""

                val hasRsvped = if (userId.isNotEmpty()) {
                    getEventRsvpStatus(userId, eventId)
                } else false

                val hasCheckedIn = if (userId.isNotEmpty()) {
                    getEventCheckInStatus(userId, eventId)
                } else false

                _state.value = EventDetailState.Success(
                    event = event,
                    hasRsvped = hasRsvped,
                    hasCheckedIn = hasCheckedIn
                )
            } catch (e: Exception) {
                _state.value = EventDetailState.Error(
                    e.message ?: "Failed to load event details"
                )
            }
        }
    }

    fun onRsvpClicked() {
        val currentState = _state.value
        if (currentState !is EventDetailState.Success) return
        if (currentState.hasRsvped) return

        val user = getCurrentUser() ?: return

        scope.launch {
            _state.update {
                (it as? EventDetailState.Success)?.copy(isRsvpLoading = true) ?: it
            }

            try {
                rsvpToEvent(user.id, eventId)
                _state.update {
                    (it as? EventDetailState.Success)?.copy(
                        hasRsvped = true,
                        isRsvpLoading = false
                    ) ?: it
                }
            } catch (e: Exception) {
                _state.update {
                    (it as? EventDetailState.Success)?.copy(isRsvpLoading = false) ?: it
                }
            }
        }
    }

    fun onCheckInClicked() {
        val currentState = _state.value
        if (currentState !is EventDetailState.Success) return
        if (!currentState.hasRsvped) return
        if (currentState.hasCheckedIn) return

        val user = getCurrentUser() ?: return

        scope.launch {
            _state.update {
                (it as? EventDetailState.Success)?.copy(isCheckInLoading = true) ?: it
            }

            try {
                checkInToEvent(user.id, eventId)
                _state.update {
                    (it as? EventDetailState.Success)?.copy(
                        hasCheckedIn = true,
                        isCheckInLoading = false
                    ) ?: it
                }
            } catch (e: Exception) {
                _state.update {
                    (it as? EventDetailState.Success)?.copy(isCheckInLoading = false) ?: it
                }
            }
        }
    }

    fun onBackClicked() {
        onBack()
    }
}