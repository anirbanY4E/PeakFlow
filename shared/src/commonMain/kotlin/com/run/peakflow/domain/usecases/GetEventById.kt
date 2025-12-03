package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Event
import com.run.peakflow.data.repository.EventRepository

class GetEventById(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Event? {
        return eventRepository.getEventById(eventId)
    }
}