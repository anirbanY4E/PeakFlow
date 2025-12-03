package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val city: String,
    val avatarUrl: String? = null,
    val interests: List<EventCategory> = emptyList(),
    val createdAt: Long,
    val isVerified: Boolean = false
)