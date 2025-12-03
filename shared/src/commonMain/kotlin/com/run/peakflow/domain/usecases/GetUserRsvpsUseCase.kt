package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.UserRepository

class GetUserRsvpsUseCase(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<Rsvp> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        return eventRepository.getUserRsvps(userId)
    }
}