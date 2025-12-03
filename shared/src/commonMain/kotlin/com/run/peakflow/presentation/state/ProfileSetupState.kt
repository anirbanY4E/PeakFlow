package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.EventCategory

data class ProfileSetupState(
    val name: String = "",
    val city: String = "Bangalore",
    val selectedInterests: List<EventCategory> = emptyList(),
    val avatarUrl: String? = null,
    val availableCities: List<String> = listOf("Bangalore"),
    val availableInterests: List<EventCategory> = EventCategory.entries,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)