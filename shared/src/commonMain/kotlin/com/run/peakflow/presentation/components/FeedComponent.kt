package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.domain.usecases.GetFeedPostsUseCase
import com.run.peakflow.domain.usecases.LikePostUseCase
import com.run.peakflow.presentation.state.FeedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FeedComponent(
    componentContext: ComponentContext,
    private val onNavigateToPostDetail: (String) -> Unit,
    private val onNavigateToCommunityDetail: (String) -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getFeedPosts: GetFeedPostsUseCase by inject()
    private val likePost: LikePostUseCase by inject()
    private val postRepository: PostRepository by inject()
    private val authRepository: com.run.peakflow.data.repository.AuthRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    // Track post IDs with in-flight like operations to avoid reload conflicts
    private val pendingLikePostIds = mutableSetOf<String>()

    // Debounce job for state change reloads
    private var reloadDebounceJob: Job? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        observeAuthState()
        observePostStateChanges()
    }

    private fun observeAuthState() {
        scope.launch {
            authRepository.authState.collect { state ->
                // If we were waiting for initialization and now we are logged in, trigger load if empty
                if (!state.isInitializing && state.isLoggedIn && _state.value.posts.isEmpty() && !_state.value.isLoading) {
                    println("FeedComponent: Auth settled and logged in, triggering loadFeed")
                    loadFeed()
                }
            }
        }
    }

    private fun observePostStateChanges() {
        scope.launch {
            postRepository.postStateChanges.collect { change ->
                // Skip reload for like changes on posts with pending like operations
                // to avoid overwriting optimistic updates with stale data
                val shouldReloadForLikes = change.likesCountChanged && change.postId !in pendingLikePostIds

                if (shouldReloadForLikes || change.commentsCountChanged || change.wasCreated) {
                    // Debounce: cancel any pending reload and schedule a new one
                    // This prevents multiple rapid-fire changes from triggering N reloads
                    reloadDebounceJob?.cancel()
                    reloadDebounceJob = scope.launch {
                        delay(500) // Wait for DB triggers to complete & coalesce rapid events
                        reloadPostsInternal()
                    }
                }
            }
        }
    }

    private fun reloadPostsInternal() {
        scope.launch {
            try {
                val currentState = _state.value
                val totalLimit = currentState.offset + 20
                val posts = getFeedPosts(limit = totalLimit, offset = 0)
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
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                // Silently fail
            }
        }
    }

    fun loadFeed() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, offset = 0, hasMorePosts = true) }
            
            try {
                // Patience: If still initializing, wait up to 3 seconds for session restoration.
                // This handles the race condition where FeedComponent is restored before AuthRepository settles.
                if (authRepository.authState.value.isInitializing) {
                    println("FeedComponent: Still initializing, waiting for auth settlement...")
                    withTimeoutOrNull(3000) {
                        authRepository.authState.first { !it.isInitializing }
                    }
                }

                val posts = getFeedPosts(limit = 20, offset = 0)
                val likedIds = posts.filter { it.isLiked }.map { it.id }.toSet()
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        posts = posts.distinctBy { p -> p.id }, 
                        likedPostIds = likedIds,
                        hasMorePosts = posts.size == 20
                    ) 
                }
            } catch (e: Exception) {
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load feed") }
            }
        }
    }

    fun loadMore() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isRefreshing || currentState.isLoadingMore || !currentState.hasMorePosts) return

        scope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val nextOffset = currentState.offset + 20
                val newPosts = getFeedPosts(limit = 20, offset = nextOffset)
                val newLikedIds = newPosts.filter { it.isLiked }.map { it.id }.toSet()
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        posts = (it.posts + newPosts).distinctBy { p -> p.id },
                        likedPostIds = it.likedPostIds + newLikedIds,
                        offset = nextOffset,
                        hasMorePosts = newPosts.size == 20
                    )
                }
            } catch (e: Exception) {
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun onRefresh() {
        scope.launch {
            _state.update { it.copy(isRefreshing = true, offset = 0, hasMorePosts = true) }
            try {
                val posts = getFeedPosts(limit = 20, offset = 0)
                val likedIds = posts.filter { it.isLiked }.map { it.id }.toSet()
                _state.update { 
                    it.copy(
                        isRefreshing = false, 
                        posts = posts.distinctBy { p -> p.id }, 
                        likedPostIds = likedIds,
                        hasMorePosts = posts.size == 20
                    ) 
                }
            } catch (e: Exception) {
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isRefreshing = false, error = e.message) }
            }
        }
    }

    fun onPostClick(postId: String) {
        onNavigateToPostDetail(postId)
    }

    fun onCommunityClick(communityId: String) {
        onNavigateToCommunityDetail(communityId)
    }

    fun onLikeClick(postId: String) {
        val currentState = _state.value
        val isCurrentlyLiked = postId in currentState.likedPostIds

        // Mark as pending to prevent reload from reverting optimistic update
        pendingLikePostIds.add(postId)

        // Optimistic update
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
            }.onFailure {
                // Revert optimistic update on failure
                pendingLikePostIds.remove(postId)
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
}
