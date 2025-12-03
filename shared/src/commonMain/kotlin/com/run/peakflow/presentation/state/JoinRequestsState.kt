package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.models.User

data class JoinRequestsState(
    val pendingRequests: List<JoinRequestWithUser> = emptyList(),
    val recentlyApproved: List<JoinRequestWithUser> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val processingRequestIds: Set<String> = emptySet()
)

data class JoinRequestWithUser(
    val request: JoinRequest,
    val user: User?
)