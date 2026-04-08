package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.domain.usecases.CancelRsvpUseCase
import com.run.peakflow.domain.usecases.CheckInToEvent
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.presentation.state.EventDetailState
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

class EventDetailComponent(
    componentContext: ComponentContext,
    private val eventId: String,
    private val onNavigateBack: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val eventRepository: EventRepository by inject()
    private val rsvpToEvent: RsvpToEvent by inject()
    private val cancelRsvp: CancelRsvpUseCase by inject()
    private val checkInToEvent: CheckInToEvent by inject()
    private val authRepository: com.run.peakflow.data.repository.AuthRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(EventDetailState())
    val state: StateFlow<EventDetailState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadEvent()
    }

    fun loadEvent() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Single RPC call replaces 4 sequential calls:
                // getEventById + getCommunityById + getEventRsvpStatus + getEventCheckInStatus
                val detail = eventRepository.getEventDetail(eventId)

                if (detail != null) {
                    // Build a minimal CommunityGroup from the RPC data for backwards compatibility
                    val community = CommunityGroup(
                        id = detail.event.groupId,
                        title = detail.communityName,
                        description = "",
                        category = detail.event.category,
                        city = "",
                        memberCount = 0,
                        createdBy = "",
                        imageUrl = detail.communityImage,
                        createdAt = 0L
                    )

                    _state.update {
                        it.copy(
                            isLoading = false,
                            event = detail.event,
                            community = community,
                            hasRsvped = detail.hasRsvped,
                            hasCheckedIn = detail.hasCheckedIn,
                            participantsCount = detail.event.currentParticipants
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Event not found") }
                }
            } catch (e: Exception) {
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
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
                if (error is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
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
                if (error is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
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
                if (error is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isCheckInLoading = false, error = error.message) }
            }
        }
    }
}