package com.run.peakflow.data.repository

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.network.ApiService

class EventRepository(
    private val api: ApiService
) {
    suspend fun getEventsByGroupId(groupId: String): List<Event> {
        return api.getEventsByGroupId(groupId)
    }

    suspend fun getEventById(eventId: String): Event? {
        return api.getEventById(eventId)
    }

    suspend fun rsvpToEvent(userId: String, eventId: String): Rsvp {
        return api.rsvpToEvent(userId, eventId)
    }

    suspend fun getUserRsvps(userId: String): List<Rsvp> {
        return api.getUserRsvps(userId)
    }

    suspend fun hasUserRsvped(userId: String, eventId: String): Boolean {
        return api.hasUserRsvped(userId, eventId)
    }

    suspend fun checkInToEvent(userId: String, eventId: String): Attendance {
        return api.checkInToEvent(userId, eventId)
    }

    suspend fun hasUserCheckedIn(userId: String, eventId: String): Boolean {
        return api.hasUserCheckedIn(userId, eventId)
    }
}