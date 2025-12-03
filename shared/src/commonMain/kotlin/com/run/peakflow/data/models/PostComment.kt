package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PostComment(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val content: String,
    val likesCount: Int = 0,
    val createdAt: Long
)