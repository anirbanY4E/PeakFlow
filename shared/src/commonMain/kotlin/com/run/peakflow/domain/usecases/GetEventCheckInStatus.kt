package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.EventRepository

class GetEventCheckInStatus(
    private val repository: EventRepository
) {
    suspend operator fun invoke(userId: String, eventId: String): Boolean {
        return repository.hasUserCheckedIn(userId, eventId)
    }
}