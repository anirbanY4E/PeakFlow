package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.Event

sealed class CommunityDetailState {
    data object Loading : CommunityDetailState()

    data class Success(
        val community: CommunityGroup,
        val events: List<Event>
    ) : CommunityDetailState()

    data class Error(val message: String) : CommunityDetailState()
}