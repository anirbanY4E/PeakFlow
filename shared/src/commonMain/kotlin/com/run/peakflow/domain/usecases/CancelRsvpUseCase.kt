package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.UserRepository

class CancelRsvpUseCase(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(eventId: String): Result<Boolean> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val success = eventRepository.cancelRsvp(userId, eventId)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to cancel RSVP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}