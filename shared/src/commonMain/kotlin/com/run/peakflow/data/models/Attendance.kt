package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Attendance(
    val id: String,
    val eventId: String,
    val userId: String,
    val checkInTimestamp: Long
)