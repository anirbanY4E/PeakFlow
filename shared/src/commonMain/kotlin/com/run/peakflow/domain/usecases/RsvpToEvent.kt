package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.repository.EventRepository

class RsvpToEvent(
    private val repository: EventRepository
) {
    suspend operator fun invoke(userId: String, eventId: String): Rsvp {
        return repository.rsvpToEvent(userId, eventId)
    }
}