package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Event
import com.run.peakflow.data.repository.EventRepository

class GetEventById(
    private val repository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Event? {
        return repository.getEventById(eventId)
    }
}