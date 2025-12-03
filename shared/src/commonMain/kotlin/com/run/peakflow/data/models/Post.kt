package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val communityId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val content: String,
    val imageUrl: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long
)