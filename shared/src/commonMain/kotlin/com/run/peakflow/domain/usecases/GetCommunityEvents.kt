package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Event
import com.run.peakflow.data.repository.EventRepository

class GetCommunityEvents(
    private val repository: EventRepository
) {
    suspend operator fun invoke(groupId: String): List<Event> {
        return repository.getEventsByGroupId(groupId)
    }
}