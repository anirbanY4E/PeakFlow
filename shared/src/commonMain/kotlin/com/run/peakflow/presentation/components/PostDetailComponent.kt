package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.domain.usecases.AddCommentUseCase
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.GetPostByIdUseCase
import com.run.peakflow.domain.usecases.GetPostCommentsUseCase
import com.run.peakflow.domain.usecases.HasUserLikedPostUseCase
import com.run.peakflow.domain.usecases.LikePostUseCase
import com.run.peakflow.presentation.state.PostDetailState
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PostDetailComponent(
    componentContext: ComponentContext,
    private val postId: String,
    private val onNavigateBack: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getPostById: GetPostByIdUseCase by inject()
    private val getCommunityById: GetCommunityById by inject()
    private val getPostComments: GetPostCommentsUseCase by inject()
    private val likePost: LikePostUseCase by inject()
    private val hasUserLikedPost: HasUserLikedPostUseCase by inject()
    private val addComment: AddCommentUseCase by inject()
    private val postRepository: PostRepository by inject()
    private val authRepository: com.run.peakflow.data.repository.AuthRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(PostDetailState())
    val state: StateFlow<PostDetailState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadPost()
        observeRealtimeComments()
    }

    fun loadPost() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Fetch post first (community depends on its communityId)
                val post = getPostById(postId)

                // Then parallelize the 3 independent calls
                val communityDeferred = async { post?.let { getCommunityById(it.communityId) } }
                val commentsDeferred = async { getPostComments(postId) }
                val hasLikedDeferred = async { hasUserLikedPost(postId) }

                val community = communityDeferred.await()
                val comments = commentsDeferred.await()
                val hasLiked = hasLikedDeferred.await()

                _state.update {
                    it.copy(
                        isLoading = false,
                        post = post,
                        community = community,
                        comments = comments,
                        hasLiked = hasLiked
                    )
                }
            } catch (e: Exception) {
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onLikeClick() {
        scope.launch {
            _state.update { it.copy(isLikeLoading = true) }

            val result = likePost(postId)

            result.onSuccess { isNowLiked ->
                _state.update { currentState ->
                    val newPost = currentState.post?.copy(
                        likesCount = if (isNowLiked) {
                            currentState.post.likesCount + 1
                        } else {
                            currentState.post.likesCount - 1
                        }
                    )
                    currentState.copy(
                        isLikeLoading = false,
                        hasLiked = isNowLiked,
                        post = newPost
                    )
                }
            }.onFailure { e ->
                if (e is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLikeLoading = false) }
            }
        }
    }

    fun onCommentTextChanged(text: String) {
        _state.update { it.copy(commentText = text) }
    }

    fun onSendCommentClick() {
        val commentText = _state.value.commentText.trim()
        if (commentText.isBlank()) return

        scope.launch {
            _state.update { it.copy(isCommentLoading = true) }

            val result = addComment(postId, commentText)

            result.onSuccess { newComment ->
                _state.update { currentState ->
                    // Dedup: realtime observer may have already added this comment
                    if (currentState.comments.any { it.id == newComment.id }) {
                        currentState.copy(
                            isCommentLoading = false,
                            commentText = ""
                        )
                    } else {
                        val newPost = currentState.post?.copy(
                            commentsCount = currentState.post.commentsCount + 1
                        )
                        currentState.copy(
                            isCommentLoading = false,
                            commentText = "",
                            comments = currentState.comments + newComment,
                            post = newPost
                        )
                    }
                }
            }.onFailure { error ->
                if (error is com.run.peakflow.data.network.AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isCommentLoading = false, error = error.message) }
            }
        }
    }

    private fun observeRealtimeComments() {
        scope.launch {
            postRepository.observeNewComments(postId).collect { newComment ->
                _state.update { currentState ->
                    // Avoid duplicates (local comments are already added optimistically)
                    if (currentState.comments.any { it.id == newComment.id }) {
                        currentState
                    } else {
                        val newPost = currentState.post?.copy(
                            commentsCount = currentState.post.commentsCount + 1
                        )
                        currentState.copy(
                            comments = currentState.comments + newComment,
                            post = newPost
                        )
                    }
                }
            }
        }
    }
}