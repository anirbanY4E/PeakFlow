package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.GetCommunityEvents
import com.run.peakflow.domain.usecases.GetCommunityMembersUseCase
import com.run.peakflow.domain.usecases.GetCommunityPostsUseCase
import com.run.peakflow.domain.usecases.GetEventRsvpStatus
import com.run.peakflow.domain.usecases.GetUserRoleInCommunityUseCase
import com.run.peakflow.domain.usecases.HasUserLikedPostUseCase
import com.run.peakflow.domain.usecases.LikePostUseCase
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.presentation.state.CommunityDetailState
import com.run.peakflow.presentation.state.CommunityTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CommunityDetailComponent(
    componentContext: ComponentContext,
    private val communityId: String,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToEventDetail: (String) -> Unit,
    private val onNavigateToPostDetail: (String) -> Unit,
    private val onNavigateToGenerateInvite: (String) -> Unit,
    private val onNavigateToJoinRequests: (String) -> Unit,
    private val onNavigateToCreateEvent: (String) -> Unit,
    private val onNavigateToCreatePost: (String) -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getCommunityById: GetCommunityById by inject()
    private val getCommunityPosts: GetCommunityPostsUseCase by inject()
    private val getCommunityEvents: GetCommunityEvents by inject()
    private val getCommunityMembers: GetCommunityMembersUseCase by inject()
    private val getUserRoleInCommunity: GetUserRoleInCommunityUseCase by inject()
    private val likePost: LikePostUseCase by inject()
    private val hasUserLikedPost: HasUserLikedPostUseCase by inject()
    private val rsvpToEvent: RsvpToEvent by inject()
    private val getEventRsvpStatus: GetEventRsvpStatus by inject()
    private val eventRepository: EventRepository by inject()
    private val postRepository: PostRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(CommunityDetailState())
    val state: StateFlow<CommunityDetailState> = _state.asStateFlow()

    init {
        loadCommunity()
        observeStateChanges()
    }

    private fun observeStateChanges() {
        // Observe event state changes
        scope.launch {
            eventRepository.eventStateChanges.collect { change ->
                _state.update { currentState ->
                    // Update RSVP status if changed
                    val newRsvpedIds = if (change.rsvpStatusChanged) {
                        val isRsvped = getEventRsvpStatus(change.eventId)
                        if (isRsvped) {
                            currentState.rsvpedEventIds + change.eventId
                        } else {
                            currentState.rsvpedEventIds - change.eventId
                        }
                    } else {
                        currentState.rsvpedEventIds
                    }

                    currentState.copy(rsvpedEventIds = newRsvpedIds)
                }
                // Reload events to get updated participant count
                if (change.participantCountChanged) {
                    reloadEventsInternal()
                }
            }
        }

        // Observe post state changes
        scope.launch {
            postRepository.postStateChanges.collect { change ->
                _state.update { currentState ->
                    // Update like status if changed
                    val newLikedIds = if (change.likeStatusChanged) {
                        val isLiked = hasUserLikedPost(change.postId)
                        if (isLiked) {
                            currentState.likedPostIds + change.postId
                        } else {
                            currentState.likedPostIds - change.postId
                        }
                    } else {
                        currentState.likedPostIds
                    }

                    currentState.copy(likedPostIds = newLikedIds)
                }
                // Reload posts to get updated counts
                if (change.likesCountChanged || change.commentsCountChanged) {
                    reloadPostsInternal()
                }
            }
        }
    }

    private fun reloadEventsInternal() {
        scope.launch {
            try {
                val events = getCommunityEvents(communityId)
                val rsvpedIds = events.map { it.id }
                    .filter { getEventRsvpStatus(it) }
                    .toSet()
                _state.update { it.copy(events = events, rsvpedEventIds = rsvpedIds) }
            } catch (e: Exception) {
                // Silently fail or log error
            }
        }
    }

    private fun reloadPostsInternal() {
        scope.launch {
            try {
                val posts = getCommunityPosts(communityId)
                val likedIds = posts.map { it.id }
                    .filter { hasUserLikedPost(it) }
                    .toSet()
                _state.update { it.copy(posts = posts, likedPostIds = likedIds) }
            } catch (e: Exception) {
                // Silently fail or log error
            }
        }
    }

    fun loadCommunity() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val community = getCommunityById(communityId)
                val posts = getCommunityPosts(communityId)
                val events = getCommunityEvents(communityId)
                val members = getCommunityMembers(communityId)
                val userRole = getUserRoleInCommunity(communityId)

                val likedIds = posts.map { it.id }
                    .filter { hasUserLikedPost(it) }
                    .toSet()

                val rsvpedIds = events.map { it.id }
                    .filter { getEventRsvpStatus(it) }
                    .toSet()

                _state.update {
                    it.copy(
                        isLoading = false,
                        community = community,
                        posts = posts,
                        events = events,
                        members = members,
                        userRole = userRole,
                        likedPostIds = likedIds,
                        rsvpedEventIds = rsvpedIds
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
            loadCommunity()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onTabSelected(tab: CommunityTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onPostClick(postId: String) {
        onNavigateToPostDetail(postId)
    }

    fun onEventClick(eventId: String) {
        onNavigateToEventDetail(eventId)
    }

    fun onLikePostClick(postId: String) {
        scope.launch {
            val result = likePost(postId)
            result.onSuccess { isNowLiked ->
                _state.update { currentState ->
                    val newLikedIds = if (isNowLiked) {
                        currentState.likedPostIds + postId
                    } else {
                        currentState.likedPostIds - postId
                    }
                    val newPosts = currentState.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                likesCount = if (isNowLiked) post.likesCount + 1 else post.likesCount - 1
                            )
                        } else post
                    }
                    currentState.copy(posts = newPosts, likedPostIds = newLikedIds)
                }
            }
        }
    }

    fun onRsvpEventClick(eventId: String) {
        scope.launch {
            val result = rsvpToEvent(eventId)
            result.onSuccess {
                _state.update { currentState ->
                    val newRsvpedIds = currentState.rsvpedEventIds + eventId
                    val newEvents = currentState.events.map { event ->
                        if (event.id == eventId) {
                            event.copy(currentParticipants = event.currentParticipants + 1)
                        } else event
                    }
                    currentState.copy(events = newEvents, rsvpedEventIds = newRsvpedIds)
                }
            }
        }
    }

    fun onGenerateInviteClick() {
        onNavigateToGenerateInvite(communityId)
    }

    fun onJoinRequestsClick() {
        onNavigateToJoinRequests(communityId)
    }

    fun onCreateEventClick() {
        onNavigateToCreateEvent(communityId)
    }

    fun onCreatePostClick() {
        onNavigateToCreatePost(communityId)
    }
}