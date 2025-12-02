package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.domain.usecases.GetNearbyCommunities
import com.run.peakflow.presentation.state.HomeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeComponent(
    componentContext: ComponentContext,
    private val getNearbyCommunities: GetNearbyCommunities,
    private val onCommunitySelected: (CommunityGroup) -> Unit
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadCommunities()
    }

    fun loadCommunities() {
        scope.launch {
            _state.value = HomeState.Loading

            try {
                val communities = getNearbyCommunities("Bangalore")
                _state.value = HomeState.Success(communities)
            } catch (e: Exception) {
                _state.value = HomeState.Error(e.message ?: "Failed to load communities")
            }
        }
    }

    fun onCommunityClicked(community: CommunityGroup) {
        onCommunitySelected(community)
    }
}