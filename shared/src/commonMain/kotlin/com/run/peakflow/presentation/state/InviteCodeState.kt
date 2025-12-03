package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup

data class InviteCodeState(
    val code: String = "",
    val isLoading: Boolean = false,
    val isValidating: Boolean = false,
    val error: String? = null,
    val validatedCommunity: CommunityGroup? = null,
    val isSuccess: Boolean = false,
    val joinedCommunityId: String? = null
)