package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.Post

data class FeedState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePosts: Boolean = true,
    val offset: Int = 0,
    val error: String? = null,
    val likedPostIds: Set<String> = emptySet()
)