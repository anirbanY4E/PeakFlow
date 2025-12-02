package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.models.Event
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.GetCommunityEvents
import com.run.peakflow.presentation.state.CommunityDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityDetailComponent(
    componentContext: ComponentContext,
    private val communityId: String,
    private val getCommunityById: GetCommunityById,
    private val getCommunityEvents: GetCommunityEvents,
    private val onEventSelected: (Event) -> Unit,
    private val onBack: () -> Unit
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<CommunityDetailState>(CommunityDetailState.Loading)
    val state: StateFlow<CommunityDetailState> = _state.asStateFlow()

    init {
        loadCommunityDetails()
    }

    fun loadCommunityDetails() {
        scope.launch {
            _state.value = CommunityDetailState.Loading

            try {
                val community = getCommunityById(communityId)

                if (community == null) {
                    _state.value = CommunityDetailState.Error("Community not found")
                    return@launch
                }

                val events = getCommunityEvents(communityId)
                _state.value = CommunityDetailState.Success(community, events)
            } catch (e: Exception) {
                _state.value = CommunityDetailState.Error(
                    e.message ?: "Failed to load community details"
                )
            }
        }
    }

    fun onEventClicked(event: Event) {
        onEventSelected(event)
    }

    fun onBackClicked() {
        onBack()
    }
}