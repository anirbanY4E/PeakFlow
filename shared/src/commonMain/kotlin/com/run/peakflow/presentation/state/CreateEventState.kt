package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.EventCategory

data class CreateEventState(
    val communityId: String = "",
    val title: String = "",
    val description: String = "",
    val category: EventCategory = EventCategory.RUNNING,
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val maxParticipants: Int = 30,
    val isFree: Boolean = true,
    val price: Double? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
