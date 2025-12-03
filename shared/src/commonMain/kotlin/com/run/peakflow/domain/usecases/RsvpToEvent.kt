package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.UserRepository

class RsvpToEvent(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(eventId: String): Result<Rsvp> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val event = eventRepository.getEventById(eventId)
                ?: return Result.failure(Exception("Event not found"))

            if (event.currentParticipants >= event.maxParticipants) {
                return Result.failure(Exception("Event is full"))
            }

            val hasRsvped = eventRepository.hasUserRsvped(userId, eventId)
            if (hasRsvped) {
                return Result.failure(Exception("Already RSVP'd to this event"))
            }

            val rsvp = eventRepository.rsvpToEvent(userId, eventId)
            Result.success(rsvp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}