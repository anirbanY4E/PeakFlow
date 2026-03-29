package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.domain.usecases.GetFeedPostsUseCase
import com.run.peakflow.domain.usecases.LikePostUseCase
import com.run.peakflow.presentation.state.FeedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    // Track post IDs with in-flight like operations to avoid reload conflicts
    private val pendingLikePostIds = mutableSetOf<String>()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadFeed()
        observePostStateChanges()
    }

    private fun observePostStateChanges() {
        scope.launch {
            postRepository.postStateChanges.collect { change ->
                // Skip reload for like changes on posts with pending like operations
                // to avoid overwriting optimistic updates with stale data
                val shouldReloadForLikes = change.likesCountChanged && change.postId !in pendingLikePostIds
                
                if (shouldReloadForLikes || change.commentsCountChanged || change.wasCreated) {
                    // Small delay to let DB triggers (likes_count, comments_count) complete
                    delay(300)
                    reloadPostsInternal()
                }
            }
        }
    }

    private fun reloadPostsInternal() {
        scope.launch {
            try {
                val posts = getFeedPosts()
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

    fun loadFeed() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val posts = getFeedPosts()
                val likedIds = posts.filter { it.isLiked }.map { it.id }.toSet()
                _state.update { it.copy(isLoading = false, posts = posts.distinctBy { p -> p.id }, likedPostIds = likedIds) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load feed") }
            }
        }
    }

    fun onRefresh() {
        scope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                val posts = getFeedPosts()
                val likedIds = posts.filter { it.isLiked }.map { it.id }.toSet()
                _state.update { it.copy(isRefreshing = false, posts = posts.distinctBy { p -> p.id }, likedPostIds = likedIds) }
            } catch (e: Exception) {
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
                // Like succeeded — remove from pending and schedule a fresh reload
                // to get accurate server counts
                pendingLikePostIds.remove(postId)
                delay(500) // Let DB trigger update likes_count
                reloadPostsInternal()
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
