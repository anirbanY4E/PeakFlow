package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val category: EventCategory,
    val date: String,
    val time: String,
    val endTime: String? = null,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val maxParticipants: Int,
    val currentParticipants: Int,
    val isFree: Boolean = true,
    val price: Double? = null,
    val createdAt: Long
)