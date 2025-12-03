package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory

data class EventsListState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedCategory: EventCategory? = null,
    val searchQuery: String = "",
    val city: String = "Bangalore",
    val rsvpedEventIds: Set<String> = emptySet()
)