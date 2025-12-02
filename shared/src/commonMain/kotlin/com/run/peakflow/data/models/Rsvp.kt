package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Rsvp(
    val id: String,
    val userId: String,
    val eventId: String,
    val timestamp: Long
)