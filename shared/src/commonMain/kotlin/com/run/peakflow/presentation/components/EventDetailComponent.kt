package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.CancelRsvpUseCase
import com.run.peakflow.domain.usecases.CheckInToEvent
import com.run.peakflow.domain.usecases.GetCommunityById
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EventDetailComponent(
    componentContext: ComponentContext,
    private val eventId: String,
    private val onNavigateBack: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getEventById: GetEventById by inject()
    private val getCommunityById: GetCommunityById by inject()
    private val getEventRsvpStatus: GetEventRsvpStatus by inject()
    private val getEventCheckInStatus: GetEventCheckInStatus by inject()
    private val rsvpToEvent: RsvpToEvent by inject()
    private val cancelRsvp: CancelRsvpUseCase by inject()
    private val checkInToEvent: CheckInToEvent by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(EventDetailState())
    val state: StateFlow<EventDetailState> = _state.asStateFlow()

    init {
        loadEvent()
    }

    fun loadEvent() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val event = getEventById(eventId)
                val community = event?.let { getCommunityById(it.groupId) }
                val hasRsvped = getEventRsvpStatus(eventId)
                val hasCheckedIn = getEventCheckInStatus(eventId)

                _state.update {
                    it.copy(
                        isLoading = false,
                        event = event,
                        community = community,
                        hasRsvped = hasRsvped,
                        hasCheckedIn = hasCheckedIn,
                        participantsCount = event?.currentParticipants ?: 0
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

    fun onRsvpClick() {
        scope.launch {
            _state.update { it.copy(isRsvpLoading = true) }

            val result = rsvpToEvent(eventId)

            result.onSuccess {
                _state.update { currentState ->
                    val newEvent = currentState.event?.copy(
                        currentParticipants = currentState.event.currentParticipants + 1
                    )
                    currentState.copy(
                        isRsvpLoading = false,
                        hasRsvped = true,
                        event = newEvent,
                        participantsCount = currentState.participantsCount + 1
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isRsvpLoading = false, error = error.message) }
            }
        }
    }

    fun onCancelRsvpClick() {
        scope.launch {
            _state.update { it.copy(isRsvpLoading = true) }

            val result = cancelRsvp(eventId)

            result.onSuccess {
                _state.update { currentState ->
                    val newEvent = currentState.event?.copy(
                        currentParticipants = (currentState.event.currentParticipants - 1).coerceAtLeast(0)
                    )
                    currentState.copy(
                        isRsvpLoading = false,
                        hasRsvped = false,
                        event = newEvent,
                        participantsCount = (currentState.participantsCount - 1).coerceAtLeast(0)
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isRsvpLoading = false, error = error.message) }
            }
        }
    }

    fun onCheckInClick() {
        scope.launch {
            _state.update { it.copy(isCheckInLoading = true) }

            val result = checkInToEvent(eventId)

            result.onSuccess {
                _state.update {
                    it.copy(
                        isCheckInLoading = false,
                        hasCheckedIn = true
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isCheckInLoading = false, error = error.message) }
            }
        }
    }
}