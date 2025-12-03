package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CommunityMembership(
    val id: String,
    val userId: String,
    val communityId: String,
    val role: MembershipRole,
    val joinedAt: Long,
    val invitedBy: String? = null
)