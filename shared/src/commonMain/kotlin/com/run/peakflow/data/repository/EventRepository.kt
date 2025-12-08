package com.run.peakflow.data.repository

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.network.ApiService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class EventStateChange(
    val eventId: String,
    val rsvpStatusChanged: Boolean = false,
    val checkInStatusChanged: Boolean = false,
    val participantCountChanged: Boolean = false,
    val newParticipantCount: Int? = null
)

class EventRepository(
    private val api: ApiService
) {
    private val _eventStateChanges = MutableSharedFlow<EventStateChange>(replay = 0)
    val eventStateChanges: SharedFlow<EventStateChange> = _eventStateChanges.asSharedFlow()
    // ==================== EVENTS ====================

    suspend fun createEvent(
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
    ): Event {
        return api.createEvent(
            communityId, title, description, category,
            date, time, location, maxParticipants, isFree, price
        )
    }

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
        val rsvp = api.rsvpToEvent(userId, eventId)
        // Emit state change
        _eventStateChanges.emit(
            EventStateChange(
                eventId = eventId,
                rsvpStatusChanged = true,
                participantCountChanged = true
            )
        )
        return rsvp
    }

    suspend fun cancelRsvp(userId: String, eventId: String): Boolean {
        val result = api.cancelRsvp(userId, eventId)
        if (result) {
            // Emit state change
            _eventStateChanges.emit(
                EventStateChange(
                    eventId = eventId,
                    rsvpStatusChanged = true,
                    participantCountChanged = true
                )
            )
        }
        return result
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
        val attendance = api.checkInToEvent(userId, eventId)
        // Emit state change
        _eventStateChanges.emit(
            EventStateChange(
                eventId = eventId,
                checkInStatusChanged = true
            )
        )
        return attendance
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