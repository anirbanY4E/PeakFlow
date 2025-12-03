package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class JoinRequest(
    val id: String,
    val userId: String,
    val communityId: String,
    val status: RequestStatus,
    val requestedAt: Long,
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null
)