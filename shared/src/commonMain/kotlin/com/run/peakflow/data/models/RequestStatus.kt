package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}