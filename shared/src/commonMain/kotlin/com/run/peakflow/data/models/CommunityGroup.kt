package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CommunityGroup(
    val id: String,
    val title: String,
    val description: String,
    val category: EventCategory,
    val city: String,
    val memberCount: Int,
    val createdBy: String,
    val imageUrl: String? = null,
    val coverUrl: String? = null,
    val rules: List<String> = emptyList(),
    val createdAt: Long
)