package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.domain.usecases.CreatePostUseCase
import com.run.peakflow.presentation.state.CreatePostState
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

class CreatePostComponent(
    componentContext: ComponentContext,
    private val communityId: String,
    private val onNavigateBack: () -> Unit,
    private val onPostCreated: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val createPost: CreatePostUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(CreatePostState(communityId = communityId))
    val state: StateFlow<CreatePostState> = _state.asStateFlow()

    fun onContentChanged(content: String) {
        _state.update { it.copy(content = content, error = null) }
    }

    fun onImageUrlChanged(imageUrl: String?) {
        _state.update { it.copy(imageUrl = imageUrl) }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onCreateClick() {
        val currentState = _state.value

        if (currentState.content.isBlank()) {
            _state.update { it.copy(error = "Post content cannot be empty") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = createPost(
                communityId = currentState.communityId,
                content = currentState.content,
                imageUrl = currentState.imageUrl
            )

            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
                onPostCreated()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
