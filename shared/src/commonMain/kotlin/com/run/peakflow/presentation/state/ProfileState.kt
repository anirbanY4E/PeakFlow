package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val stats: ProfileStats = ProfileStats(),
    val interests: List<EventCategory> = emptyList()
)

data class ProfileStats(
    val communitiesCount: Int = 0,
    val eventsAttended: Int = 0,
    val points: Int = 0
)