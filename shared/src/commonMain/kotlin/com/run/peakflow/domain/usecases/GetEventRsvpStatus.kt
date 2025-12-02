package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.EventRepository

class GetEventRsvpStatus(
    private val repository: EventRepository
) {
    suspend operator fun invoke(userId: String, eventId: String): Boolean {
        return repository.hasUserRsvped(userId, eventId)
    }
}