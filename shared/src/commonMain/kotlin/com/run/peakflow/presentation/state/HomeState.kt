package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup

sealed class HomeState {
    data object Loading : HomeState()
    data class Success(val communities: List<CommunityGroup>) : HomeState()
    data class Error(val message: String) : HomeState()
}