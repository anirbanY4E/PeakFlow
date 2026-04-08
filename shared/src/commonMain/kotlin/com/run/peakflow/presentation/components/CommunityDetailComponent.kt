package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.AuthenticationException
import com.run.peakflow.data.repository.AuthRepository
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.GetCommunityPostsUseCase
import com.run.peakflow.domain.usecases.GetEventRsvpStatus
import com.run.peakflow.domain.usecases.GetUserRoleInCommunityUseCase
import com.run.peakflow.domain.usecases.LikePostUseCase
import com.run.peakflow.domain.usecases.RequestToJoinCommunityUseCase
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.presentation.state.CommunityDetailState
import com.run.peakflow.presentation.state.CommunityTab
import com.run.peakflow.presentation.state.MemberWithUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
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
    private val initialTab: CommunityTab = CommunityTab.POSTS,
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
    private val getUserRoleInCommunity: GetUserRoleInCommunityUseCase by inject()
    private val likePost: LikePostUseCase by inject()
    private val rsvpToEvent: RsvpToEvent by inject()
    private val requestToJoinCommunity: RequestToJoinCommunityUseCase by inject()
    private val getEventRsvpStatus: GetEventRsvpStatus by inject()
    private val eventRepository: EventRepository by inject()
    private val membershipRepository: MembershipRepository by inject()
    private val postRepository: PostRepository by inject()
    private val userRepository: com.run.peakflow.data.repository.UserRepository by inject()
    private val authRepository: AuthRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(CommunityDetailState(selectedTab = initialTab))
    val state: StateFlow<CommunityDetailState> = _state.asStateFlow()

    // Track post IDs with in-flight like operations to avoid reload conflicts
    private val pendingLikePostIds = mutableSetOf<String>()

    // Debounce jobs for state change reloads
    private var eventsReloadDebounceJob: Job? = null
    private var postsReloadDebounceJob: Job? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadCommunity()
        observeStateChanges()
        observeRealtimePosts()
    }

    private fun observeStateChanges() {
        // Observe event state changes
        scope.launch {
            eventRepository.eventStateChanges.collect { change ->
                _state.update { currentState ->
                    // Update RSVP status if changed
                    val newRsvpedIds = if (change.rsvpStatusChanged && change.isRsvped != null) {
                        if (change.isRsvped == true) {
                            currentState.rsvpedEventIds + change.eventId
                        } else {
                            currentState.rsvpedEventIds - change.eventId
                        }
                    } else {
                        currentState.rsvpedEventIds
                    }

                    currentState.copy(rsvpedEventIds = newRsvpedIds)
                }
                // Debounce reload for event changes
                if (change.participantCountChanged || change.wasCreated) {
                    eventsReloadDebounceJob?.cancel()
                    eventsReloadDebounceJob = scope.launch {
                        delay(500)
                        reloadEventsInternal()
                    }
                }
            }
        }

        // Observe post state changes
        scope.launch {
            postRepository.postStateChanges.collect { change ->
                // Skip reload for like changes on posts with pending like operations
                // to avoid overwriting optimistic updates with stale data
                val shouldReloadForLikes = change.likesCountChanged && change.postId !in pendingLikePostIds

                if (shouldReloadForLikes || change.commentsCountChanged || change.wasCreated) {
                    // Debounce: coalesce rapid-fire changes into a single reload
                    postsReloadDebounceJob?.cancel()
                    postsReloadDebounceJob = scope.launch {
                        delay(500)
                        reloadPostsInternal()
                    }
                }
            }
        }
    }

    private fun reloadEventsInternal() {
        scope.launch {
            try {
                val eventsWithRsvp = eventRepository.getCommunityEventsWithRsvp(communityId)
                val events = eventsWithRsvp.map { it.first }
                val rsvpedIds = eventsWithRsvp.filter { it.second }.map { it.first.id }.toSet()
                _state.update { it.copy(events = events, rsvpedEventIds = rsvpedIds) }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun reloadPostsInternal() {
        scope.launch {
            try {
                val currentState = _state.value
                val totalLimit = currentState.postsOffset + 20
                val posts = getCommunityPosts(communityId, limit = totalLimit, offset = 0)
                // Use is_liked from the RPC response
                val likedIds = posts.filter { it.isLiked }.map { it.id }.toSet()
                _state.update { currentState ->
                    // Preserve optimistic like state for pending operations
                    val mergedLikedIds = likedIds.toMutableSet()
                    val mergedPosts = posts.map { post ->
                        if (post.id in pendingLikePostIds) {
                            // Keep the optimistic state for posts with pending likes
                            val isOptimisticallyLiked = post.id in currentState.likedPostIds
                            if (isOptimisticallyLiked != post.isLiked) {
                                if (isOptimisticallyLiked) mergedLikedIds.add(post.id) else mergedLikedIds.remove(post.id)
                                post.copy(
                                    likesCount = if (isOptimisticallyLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                                )
                            } else {
                                post
                            }
                        } else {
                            post
                        }
                    }.distinctBy { it.id } // Defensive: prevent duplicate key crash
                    currentState.copy(posts = mergedPosts, likedPostIds = mergedLikedIds)
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun observeRealtimePosts() {
        scope.launch {
            try {
                postRepository.observeNewPosts(communityId).collect { newPost ->
                    _state.update { currentState ->
                        // Avoid duplicates (local creates are already handled by postStateChanges)
                        if (currentState.posts.any { it.id == newPost.id }) {
                            currentState
                        } else {
                            // Prepend + defensive dedup to prevent duplicate key crash
                            val updated = (listOf(newPost) + currentState.posts).distinctBy { it.id }
                            currentState.copy(posts = updated)
                        }
                    }
                }
            } catch (_: Exception) {
                // Realtime observation failed — non-fatal, data will refresh on pull-to-refresh
            }
        }
    }

    fun loadCommunity() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, postsOffset = 0, hasMorePosts = true) }

            try {
                // Parallelize all 4 independent fetches
                val communityDeferred = async { getCommunityById(communityId) }
                val postsDeferred = async { getCommunityPosts(communityId, limit = 20, offset = 0) }
                val eventsWithRsvpDeferred = async { eventRepository.getCommunityEventsWithRsvp(communityId) }
                val membersDeferred = async { membershipRepository.getCommunityMembersWithProfiles(communityId) }
                val userRoleDeferred = async { getUserRoleInCommunity(communityId) }
                val pendingJoinRequestDeferred = async { 
                    val userId = userRepository.getCurrentUserId()
                    if (userId != null && userRoleDeferred.await() == null) {
                        membershipRepository.hasUserRequestedToJoin(userId, communityId)
                    } else false
                }

                // Await all in parallel
                val community = communityDeferred.await()
                val posts = postsDeferred.await()
                val eventsWithRsvp = eventsWithRsvpDeferred.await()
                val membersWithProfiles = membersDeferred.await()
                val userRole = userRoleDeferred.await()

                // Use is_liked from the get_community_posts RPC — no N+1!
                val likedIds = posts.filter { it.isLiked }.map { it.id }.toSet()

                // Events + RSVP from the batch RPC — no N+1!
                val events = eventsWithRsvp.map { it.first }
                val rsvpedIds = eventsWithRsvp.filter { it.second }.map { it.first.id }.toSet()

                // Members already have profile info from the batch RPC — no N+1!
                val membersWithUsers = membersWithProfiles.map { mwp ->
                    MemberWithUser(
                        membership = mwp.membership,
                        user = User(
                            id = mwp.membership.userId,
                            name = mwp.userName,
                            email = mwp.userEmail,
                            phone = null,
                            city = "",
                            avatarUrl = mwp.userAvatarUrl,
                            interests = emptyList(),
                            createdAt = 0L,
                            isVerified = false
                        )
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        community = community,
                        posts = posts.distinctBy { p -> p.id }, // Defensive dedup
                        hasMorePosts = posts.size == 20,
                        events = events,
                        members = membersWithUsers,
                        userRole = userRole,
                        likedPostIds = likedIds,
                        rsvpedEventIds = rsvpedIds,
                        hasPendingJoinRequest = pendingJoinRequestDeferred.await()
                    )
                }
            } catch (e: Exception) {
                if (e is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMorePosts() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isRefreshing || currentState.isPostsLoadingMore || !currentState.hasMorePosts) return

        scope.launch {
            _state.update { it.copy(isPostsLoadingMore = true) }
            try {
                val nextOffset = currentState.postsOffset + 20
                val newPosts = getCommunityPosts(communityId, limit = 20, offset = nextOffset)
                val newLikedIds = newPosts.filter { it.isLiked }.map { it.id }.toSet()
                _state.update {
                    it.copy(
                        isPostsLoadingMore = false,
                        posts = (it.posts + newPosts).distinctBy { p -> p.id },
                        likedPostIds = it.likedPostIds + newLikedIds,
                        postsOffset = nextOffset,
                        hasMorePosts = newPosts.size == 20
                    )
                }
            } catch (e: Exception) {
                if (e is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isPostsLoadingMore = false, error = e.message) }
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
        val currentState = _state.value
        val isCurrentlyLiked = postId in currentState.likedPostIds

        // Mark as pending to prevent reload from reverting optimistic update
        pendingLikePostIds.add(postId)

        // Optimistic update — immediately update UI
        _state.update { state ->
            val newLikedIds = if (isCurrentlyLiked) {
                state.likedPostIds - postId
            } else {
                state.likedPostIds + postId
            }
            val newPosts = state.posts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        likesCount = if (isCurrentlyLiked) (post.likesCount - 1).coerceAtLeast(0) else post.likesCount + 1,
                        isLiked = !isCurrentlyLiked
                    )
                } else post
            }
            state.copy(posts = newPosts, likedPostIds = newLikedIds)
        }

        scope.launch {
            val result = likePost(postId)
            result.onSuccess {
                // Like succeeded — clear pending flag.
                // The postStateChanges observer will debounce and reload once.
                pendingLikePostIds.remove(postId)
            }.onFailure { e ->
                // Revert optimistic update on failure
                pendingLikePostIds.remove(postId)
                if (e is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { state ->
                    val revertedLikedIds = if (isCurrentlyLiked) {
                        state.likedPostIds + postId
                    } else {
                        state.likedPostIds - postId
                    }
                    val revertedPosts = state.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                likesCount = if (isCurrentlyLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0),
                                isLiked = isCurrentlyLiked
                            )
                        } else post
                    }
                    state.copy(posts = revertedPosts, likedPostIds = revertedLikedIds)
                }
            }
        }
    }

    fun onRsvpEventClick(eventId: String) {
        _state.update { it.copy(rsvpingEventIds = it.rsvpingEventIds + eventId) }
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
                    currentState.copy(events = newEvents, rsvpedEventIds = newRsvpedIds, rsvpingEventIds = currentState.rsvpingEventIds - eventId)
                }
            }.onFailure { error ->
                if (error is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(rsvpingEventIds = it.rsvpingEventIds - eventId) }
            }
        }
    }

    fun onGenerateInviteClick() {
        onNavigateToGenerateInvite(communityId)
    }

    fun onJoinRequestsClick() {
        onNavigateToJoinRequests(communityId)
    }

    fun onJoinCommunityClick() {
        _state.update { it.copy(isJoining = true) }
        scope.launch {
            val result = requestToJoinCommunity(communityId)
            result.onSuccess {
                _state.update { it.copy(isJoining = false, hasPendingJoinRequest = true) }
                loadCommunity()
            }.onFailure { error ->
                if (error is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isJoining = false) }
            }
        }
    }

    fun onCreateEventClick() {
        onNavigateToCreateEvent(communityId)
    }

    fun onCreatePostClick() {
        onNavigateToCreatePost(communityId)
    }
}
