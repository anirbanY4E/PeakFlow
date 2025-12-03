package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Event
import com.run.peakflow.data.repository.EventRepository

class GetCommunityEvents(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(communityId: String): List<Event> {
        return eventRepository.getEventsByGroupId(communityId)
    }
}