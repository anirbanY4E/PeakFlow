package com.run.peakflow.presentation.state

data class CreatePostState(
    val communityId: String = "",
    val content: String = "",
    val imageBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
