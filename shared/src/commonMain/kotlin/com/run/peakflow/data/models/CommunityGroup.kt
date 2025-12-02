package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CommunityGroup(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val city: String,
    val memberCount: Int,
    val createdBy: String
)