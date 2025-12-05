package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.domain.usecases.CreateEventUseCase
import com.run.peakflow.presentation.state.CreateEventState
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

class CreateEventComponent(
    componentContext: ComponentContext,
    private val communityId: String,
    private val onNavigateBack: () -> Unit,
    private val onEventCreated: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val createEvent: CreateEventUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(CreateEventState(communityId = communityId))
    val state: StateFlow<CreateEventState> = _state.asStateFlow()

    fun onTitleChanged(title: String) {
        _state.update { it.copy(title = title, error = null) }
    }

    fun onDescriptionChanged(description: String) {
        _state.update { it.copy(description = description) }
    }

    fun onCategoryChanged(category: EventCategory) {
        _state.update { it.copy(category = category) }
    }

    fun onDateChanged(date: String) {
        _state.update { it.copy(date = date, error = null) }
    }

    fun onTimeChanged(time: String) {
        _state.update { it.copy(time = time, error = null) }
    }

    fun onLocationChanged(location: String) {
        _state.update { it.copy(location = location, error = null) }
    }

    fun onMaxParticipantsChanged(max: Int) {
        _state.update { it.copy(maxParticipants = max) }
    }

    fun onIsFreeChanged(isFree: Boolean) {
        _state.update { it.copy(isFree = isFree, price = if (isFree) null else it.price) }
    }

    fun onPriceChanged(price: Double?) {
        _state.update { it.copy(price = price) }
    }

    fun onBackClick() {
        onNavigateBack()
    }

    fun onCreateClick() {
        val currentState = _state.value

        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = createEvent(
                communityId = currentState.communityId,
                title = currentState.title,
                description = currentState.description,
                category = currentState.category,
                date = currentState.date,
                time = currentState.time,
                location = currentState.location,
                maxParticipants = currentState.maxParticipants,
                isFree = currentState.isFree,
                price = currentState.price
            )

            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
                onEventCreated()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
