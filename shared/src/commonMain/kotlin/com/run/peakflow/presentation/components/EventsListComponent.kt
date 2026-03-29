package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.presentation.state.EventsListState
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

class EventsListComponent(
    componentContext: ComponentContext,
    private val onNavigateToEventDetail: (String) -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val rsvpToEvent: RsvpToEvent by inject()
    private val eventRepository: EventRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(EventsListState())
    val state: StateFlow<EventsListState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadEvents()
        observeEventStateChanges()
    }

    private fun observeEventStateChanges() {
        scope.launch {
            eventRepository.eventStateChanges.collect { change ->
                // Reload events if anything significant changed
                if (change.rsvpStatusChanged || change.participantCountChanged || change.wasCreated) {
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
                // Single RPC call: replaces getUserMemberships + getNearbyEvents + N × hasUserRsvped
                val eventsWithRsvp = eventRepository.getUserEventsWithRsvp(currentState.selectedCategory)
                val events = eventsWithRsvp.map { it.first }
                val rsvpedIds = eventsWithRsvp.filter { it.second }.map { it.first.id }.toSet()

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
                val eventsWithRsvp = eventRepository.getUserEventsWithRsvp(currentState.selectedCategory)
                val events = eventsWithRsvp.map { it.first }
                val rsvpedIds = eventsWithRsvp.filter { it.second }.map { it.first.id }.toSet()

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