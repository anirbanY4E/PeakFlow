package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PostLike(
    val id: String,
    val postId: String,
    val userId: String,
    val createdAt: Long
)