package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class CreateEventUseCase(
    private val eventRepository: EventRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        communityId: String,
        title: String,
        description: String,
        category: EventCategory,
        date: String,
        time: String,
        location: String,
        maxParticipants: Int,
        isFree: Boolean = true,
        price: Double? = null
    ): Result<Event> {
        // Step 1: Verify user is logged in
        val userId = userRepository.getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        // Step 2: Verify user is admin of this community
        val membership = membershipRepository.getMembershipRole(userId, communityId)
        if (membership?.role != MembershipRole.ADMIN) {
            return Result.failure(Exception("Only admins can create events"))
        }

        // Step 3: Validate inputs
        if (title.isBlank()) {
            return Result.failure(Exception("Event title is required"))
        }
        if (date.isBlank()) {
            return Result.failure(Exception("Event date is required"))
        }
        if (time.isBlank()) {
            return Result.failure(Exception("Event time is required"))
        }
        if (location.isBlank()) {
            return Result.failure(Exception("Event location is required"))
        }
        if (maxParticipants < 2) {
            return Result.failure(Exception("Minimum 2 participants required"))
        }

        // Step 4: Create event
        return try {
            val event = eventRepository.createEvent(
                communityId = communityId,
                title = title,
                description = description,
                category = category,
                date = date,
                time = time,
                location = location,
                maxParticipants = maxParticipants,
                isFree = isFree,
                price = price
            )
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
