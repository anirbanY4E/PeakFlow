package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.Event

sealed class EventDetailState {
    data object Loading : EventDetailState()

    data class Success(
        val event: Event,
        val hasRsvped: Boolean,
        val hasCheckedIn: Boolean,
        val isRsvpLoading: Boolean = false,
        val isCheckInLoading: Boolean = false
    ) : EventDetailState()

    data class Error(val message: String) : EventDetailState()
}