package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.presentation.state.EventsListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EventsListComponent(
    componentContext: ComponentContext,
    private val onNavigateToEventDetail: (String) -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val rsvpToEvent: RsvpToEvent by inject()
    private val eventRepository: EventRepository by inject()
    private val authRepository: com.run.peakflow.data.repository.AuthRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(EventsListState())
    val state: StateFlow<EventsListState> = _state.asStateFlow()

    // Debounce job for state change reloads
    private var reloadDebounceJob: Job? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        observeAuthState()
        observeEventStateChanges()
    }

    private fun observeAuthState() {
        scope.launch {
            authRepository.authState.collect { state ->
                // If we were waiting for initialization and now we are logged in, trigger load if empty
                if (!state.isInitializing && state.isLoggedIn && _state.value.events.isEmpty() && !_state.value.isLoading) {
                    println("EventsListComponent: Auth settled and logged in, triggering loadEvents")
                    loadEvents()
                }
            }
        }
    }

    private fun observeEventStateChanges() {
        scope.launch {
            eventRepository.eventStateChanges.collect { change ->
                _state.update { currentState ->
                    val newRsvpedIds = if (change.rsvpStatusChanged && change.isRsvped != null) {
                        if (change.isRsvped == true) {
                            currentState.rsvpedEventIds + change.eventId
                        } else {
                            currentState.rsvpedEventIds - change.eventId
                        }
                    } else currentState.rsvpedEventIds

                    val newEvents = currentState.events.map { event ->
                        if (event.id == change.eventId && change.rsvpStatusChanged && change.isRsvped != null) {
                            val currentP = event.currentParticipants
                            event.copy(currentParticipants = if (change.isRsvped == true) currentP + 1 else if (change.isRsvped == false) currentP - 1 else currentP)
                        } else event
                    }
                    currentState.copy(rsvpedEventIds = newRsvpedIds, events = newEvents)
                }

                // Debounce: coalesce rapid-fire changes into a single reload
                if (change.participantCountChanged || change.wasCreated) {
                    reloadDebounceJob?.cancel()
                    reloadDebounceJob = scope.launch {
                        delay(500)
                        loadEvents()
                    }
                }
            }
        }
    }

    fun loadEvents() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Patience: If still initializing, wait up to 3 seconds for session restoration.
                if (authRepository.authState.value.isInitializing) {
                    println("EventsListComponent: Still initializing, waiting for auth settlement...")
                    withTimeoutOrNull(3000) {
                        authRepository.authState.first { !it.isInitializing }
                    }
                }

                if (!authRepository.authState.value.isLoggedIn) {
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }

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
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onRefresh() {
        scope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                // Patience: If still initializing, wait up to 3 seconds for session restoration.
                if (authRepository.authState.value.isInitializing) {
                    println("EventsListComponent: Still initializing, waiting for auth settlement...")
                    withTimeoutOrNull(3000) {
                        authRepository.authState.first { !it.isInitializing }
                    }
                }

                if (!authRepository.authState.value.isLoggedIn) {
                    _state.update { it.copy(isRefreshing = false) }
                    return@launch
                }

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
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
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
        _state.update { it.copy(rsvpingEventIds = it.rsvpingEventIds + eventId) }
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
                    currentState.copy(events = newEvents, rsvpedEventIds = newRsvpedIds, rsvpingEventIds = currentState.rsvpingEventIds - eventId)
                }
            }
            result.onFailure {
                _state.update { it.copy(rsvpingEventIds = it.rsvpingEventIds - eventId) }
            }
        }
    }
}