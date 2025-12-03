package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.Post
import com.run.peakflow.data.models.PostComment

data class PostDetailState(
    val post: Post? = null,
    val community: CommunityGroup? = null,
    val comments: List<PostComment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLiked: Boolean = false,
    val isLikeLoading: Boolean = false,
    val commentText: String = "",
    val isCommentLoading: Boolean = false
)