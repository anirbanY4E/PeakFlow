package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.domain.usecases.GetEventRsvpStatus
import com.run.peakflow.domain.usecases.GetNearbyEventsUseCase
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.presentation.state.EventsListState
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

class EventsListComponent(
    componentContext: ComponentContext,
    private val onNavigateToEventDetail: (String) -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getNearbyEvents: GetNearbyEventsUseCase by inject()
    private val rsvpToEvent: RsvpToEvent by inject()
    private val getEventRsvpStatus: GetEventRsvpStatus by inject()
    private val eventRepository: EventRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(EventsListState())
    val state: StateFlow<EventsListState> = _state.asStateFlow()

    init {
        loadEvents()
        observeEventStateChanges()
    }

    private fun observeEventStateChanges() {
        scope.launch {
            eventRepository.eventStateChanges.collect { change ->
                _state.update { currentState ->
                    // Update RSVP status if changed
                    val newRsvpedIds = if (change.rsvpStatusChanged) {
                        val isRsvped = getEventRsvpStatus(change.eventId)
                        if (isRsvped) {
                            currentState.rsvpedEventIds + change.eventId
                        } else {
                            currentState.rsvpedEventIds - change.eventId
                        }
                    } else {
                        currentState.rsvpedEventIds
                    }

                    // Update participant count if changed
                    val newEvents = if (change.participantCountChanged) {
                        currentState.events.map { event ->
                            if (event.id == change.eventId) {
                                // Reload the event to get updated participant count
                                event
                            } else {
                                event
                            }
                        }
                    } else {
                        currentState.events
                    }

                    currentState.copy(
                        rsvpedEventIds = newRsvpedIds,
                        events = newEvents
                    )
                }
                // Reload events to get updated data
                if (change.participantCountChanged) {
                    loadEvents()
                }
            }
        }
    }

    fun loadEvents() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val currentState = _state.value
                val events = getNearbyEvents(currentState.city, currentState.selectedCategory)
                val rsvpedIds = events.map { it.id }
                    .filter { getEventRsvpStatus(it) }
                    .toSet()

                _state.update {
                    it.copy(
                        isLoading = false,
                        events = events,
                        rsvpedEventIds = rsvpedIds
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

            try {
                val currentState = _state.value
                val events = getNearbyEvents(currentState.city, currentState.selectedCategory)
                val rsvpedIds = events.map { it.id }
                    .filter { getEventRsvpStatus(it) }
                    .toSet()

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        events = events,
                        rsvpedEventIds = rsvpedIds
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false, error = e.message) }
            }
        }
    }

    fun onCategorySelected(category: EventCategory?) {
        _state.update { it.copy(selectedCategory = category) }
        loadEvents()
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onEventClick(eventId: String) {
        onNavigateToEventDetail(eventId)
    }

    fun onRsvpClick(eventId: String) {
        scope.launch {
            val result = rsvpToEvent(eventId)
            result.onSuccess {
                _state.update { currentState ->
                    val newRsvpedIds = currentState.rsvpedEventIds + eventId
                    val newEvents = currentState.events.map { event ->
                        if (event.id == eventId) {
                            event.copy(currentParticipants = event.currentParticipants + 1)
                        } else event
                    }
                    currentState.copy(events = newEvents, rsvpedEventIds = newRsvpedIds)
                }
            }
        }
    }
}