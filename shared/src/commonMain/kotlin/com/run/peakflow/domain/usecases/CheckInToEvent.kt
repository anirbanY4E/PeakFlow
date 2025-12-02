package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.repository.EventRepository

class CheckInToEvent(
    private val repository: EventRepository
) {
    suspend operator fun invoke(userId: String, eventId: String): Attendance {
        return repository.checkInToEvent(userId, eventId)
    }
}