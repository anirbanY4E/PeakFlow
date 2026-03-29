package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository
import com.run.peakflow.domain.usecases.GetDiscoverCommunitiesUseCase
import com.run.peakflow.domain.usecases.GetUserCommunitiesUseCase
import com.run.peakflow.domain.usecases.RequestToJoinCommunityUseCase
import com.run.peakflow.presentation.state.CommunitiesListState
import com.run.peakflow.presentation.state.CommunitiesTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CommunitiesListComponent(
    componentContext: ComponentContext,
    private val onNavigateToCommunityDetail: (String) -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getUserCommunities: GetUserCommunitiesUseCase by inject()
    private val getDiscoverCommunities: GetDiscoverCommunitiesUseCase by inject()
    private val requestToJoinCommunity: RequestToJoinCommunityUseCase by inject()
    private val membershipRepository: MembershipRepository by inject()
    private val userRepository: UserRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(CommunitiesListState())
    val state: StateFlow<CommunitiesListState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadCommunities()
    }

    fun loadCommunities() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Parallelize both fetches
                val myGroupsDeferred = async { getUserCommunities() }
                val discoverGroupsDeferred = async { getDiscoverCommunities("Bangalore") }

                val myGroups = myGroupsDeferred.await()
                val discoverGroups = discoverGroupsDeferred.await()

                // Batch-fetch pending join request community IDs in ONE query instead of N+1
                val userId = userRepository.getCurrentUserId()
                val pendingIds = if (userId != null) {
                    membershipRepository.getPendingJoinRequestCommunityIds(userId)
                } else {
                    emptySet()
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        myGroups = myGroups,
                        discoverGroups = discoverGroups,
                        pendingRequestCommunityIds = pendingIds
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onRefresh() {
        scope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                val myGroupsDeferred = async { getUserCommunities() }
                val discoverGroupsDeferred = async { getDiscoverCommunities("Bangalore") }

                val myGroups = myGroupsDeferred.await()
                val discoverGroups = discoverGroupsDeferred.await()

                val userId = userRepository.getCurrentUserId()
                val pendingIds = if (userId != null) {
                    membershipRepository.getPendingJoinRequestCommunityIds(userId)
                } else {
                    emptySet()
                }

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        myGroups = myGroups,
                        discoverGroups = discoverGroups,
                        pendingRequestCommunityIds = pendingIds
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false, error = e.message) }
            }
        }
    }

    fun onTabSelected(tab: CommunitiesTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onCommunityClick(communityId: String) {
        onNavigateToCommunityDetail(communityId)
    }

    fun onRequestToJoinClick(communityId: String) {
        scope.launch {
            val result = requestToJoinCommunity(communityId)
            result.onSuccess {
                _state.update { currentState ->
                    currentState.copy(
                        pendingRequestCommunityIds = currentState.pendingRequestCommunityIds + communityId
                    )
                }
            }
        }
    }
}