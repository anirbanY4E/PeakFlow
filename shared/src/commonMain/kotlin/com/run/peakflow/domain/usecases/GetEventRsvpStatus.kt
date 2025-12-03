package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.UserRepository

class GetEventRsvpStatus(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(eventId: String): Boolean {
        val userId = userRepository.getCurrentUserId() ?: return false
        return eventRepository.hasUserRsvped(userId, eventId)
    }
}