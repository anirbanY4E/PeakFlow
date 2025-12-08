package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.domain.usecases.GetFeedPostsUseCase
import com.run.peakflow.domain.usecases.HasUserLikedPostUseCase
import com.run.peakflow.domain.usecases.LikePostUseCase
import com.run.peakflow.presentation.state.FeedState
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

class FeedComponent(
    componentContext: ComponentContext,
    private val onNavigateToPostDetail: (String) -> Unit,
    private val onNavigateToCommunityDetail: (String) -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getFeedPosts: GetFeedPostsUseCase by inject()
    private val likePost: LikePostUseCase by inject()
    private val hasUserLikedPost: HasUserLikedPostUseCase by inject()
    private val postRepository: PostRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        loadFeed()
        observePostStateChanges()
    }

    private fun observePostStateChanges() {
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

    private fun reloadPostsInternal() {
        scope.launch {
            try {
                val posts = getFeedPosts()
                val likedIds = posts.map { it.id }
                    .filter { hasUserLikedPost(it) }
                    .toSet()
                _state.update { it.copy(posts = posts, likedPostIds = likedIds) }
            } catch (e: Exception) {
                // Silently fail or log error
            }
        }
    }

    fun loadFeed() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val posts = getFeedPosts()
                val likedIds = posts.map { it.id }
                    .filter { hasUserLikedPost(it) }
                    .toSet()

                _state.update {
                    it.copy(
                        isLoading = false,
                        posts = posts,
                        likedPostIds = likedIds
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
                val posts = getFeedPosts()
                val likedIds = posts.map { it.id }
                    .filter { hasUserLikedPost(it) }
                    .toSet()

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        posts = posts,
                        likedPostIds = likedIds
                    )
                }
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
}