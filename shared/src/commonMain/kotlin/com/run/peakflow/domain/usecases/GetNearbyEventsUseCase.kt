package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class GetNearbyEventsUseCase(
    private val eventRepository: EventRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        city: String,
        category: EventCategory? = null
    ): List<Event> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        val memberships = membershipRepository.getUserMemberships(userId)
        val communityIds = memberships.map { it.communityId }
        return eventRepository.getNearbyEvents(city, category, communityIds)
    }
}