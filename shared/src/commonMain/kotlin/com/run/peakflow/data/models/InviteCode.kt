package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class InviteCode(
    val id: String,
    val code: String,
    val communityId: String,
    val createdBy: String,
    val maxUses: Int? = null,
    val currentUses: Int = 0,
    val expiresAt: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long
)