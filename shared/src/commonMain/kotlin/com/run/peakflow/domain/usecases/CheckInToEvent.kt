package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.UserRepository

class CheckInToEvent(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(eventId: String): Result<Attendance> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val hasRsvped = eventRepository.hasUserRsvped(userId, eventId)
            if (!hasRsvped) {
                return Result.failure(Exception("You must RSVP before checking in"))
            }

            val hasCheckedIn = eventRepository.hasUserCheckedIn(userId, eventId)
            if (hasCheckedIn) {
                return Result.failure(Exception("Already checked in"))
            }

            val attendance = eventRepository.checkInToEvent(userId, eventId)
            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}