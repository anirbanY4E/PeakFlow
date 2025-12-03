package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class MembershipRole {
    ADMIN,
    MEMBER,
    PENDING
}