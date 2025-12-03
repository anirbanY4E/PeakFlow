package com.run.peakflow.data.repository

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.network.ApiService

class EventRepository(
    private val api: ApiService
) {
    // ==================== EVENTS ====================

    suspend fun getEventsByGroupId(groupId: String): List<Event> {
        return api.getEventsByGroupId(groupId)
    }

    suspend fun getEventById(eventId: String): Event? {
        return api.getEventById(eventId)
    }

    suspend fun getNearbyEvents(
        city: String,
        category: EventCategory? = null,
        communityIds: List<String>
    ): List<Event> {
        return api.getNearbyEvents(city, category, communityIds)
    }

    suspend fun getAllAccessibleEvents(communityIds: List<String>): List<Event> {
        return api.getAllAccessibleEvents(communityIds)
    }

    // ==================== RSVP ====================

    suspend fun rsvpToEvent(userId: String, eventId: String): Rsvp {
        return api.rsvpToEvent(userId, eventId)
    }

    suspend fun cancelRsvp(userId: String, eventId: String): Boolean {
        return api.cancelRsvp(userId, eventId)
    }

    suspend fun getUserRsvps(userId: String): List<Rsvp> {
        return api.getUserRsvps(userId)
    }

    suspend fun getEventRsvps(eventId: String): List<Rsvp> {
        return api.getEventRsvps(eventId)
    }

    suspend fun hasUserRsvped(userId: String, eventId: String): Boolean {
        return api.hasUserRsvped(userId, eventId)
    }

    // ==================== ATTENDANCE ====================

    suspend fun checkInToEvent(userId: String, eventId: String): Attendance {
        return api.checkInToEvent(userId, eventId)
    }

    suspend fun hasUserCheckedIn(userId: String, eventId: String): Boolean {
        return api.hasUserCheckedIn(userId, eventId)
    }

    suspend fun getEventAttendance(eventId: String): List<Attendance> {
        return api.getEventAttendance(eventId)
    }

    suspend fun getUserAttendanceHistory(userId: String): List<Attendance> {
        return api.getUserAttendanceHistory(userId)
    }
}