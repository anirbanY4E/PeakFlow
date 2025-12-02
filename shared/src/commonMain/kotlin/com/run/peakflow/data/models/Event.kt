package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val groupId: String,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val description: String,
    val maxParticipants: Int,
    val currentParticipants: Int
)