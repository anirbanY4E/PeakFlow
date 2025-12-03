package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.User

data class EventDetailState(
    val event: Event? = null,
    val community: CommunityGroup? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasRsvped: Boolean = false,
    val hasCheckedIn: Boolean = false,
    val isRsvpLoading: Boolean = false,
    val isCheckInLoading: Boolean = false,
    val participants: List<User> = emptyList(),
    val participantsCount: Int = 0
)