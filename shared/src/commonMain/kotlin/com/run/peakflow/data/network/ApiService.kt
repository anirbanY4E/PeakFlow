package com.run.peakflow.data.network

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.models.User

interface ApiService {

    // User
    suspend fun createUser(name: String, city: String): User
    suspend fun getUser(userId: String): User?

    // Communities
    suspend fun getCommunities(): List<CommunityGroup>
    suspend fun getCommunitiesByCity(city: String): List<CommunityGroup>
    suspend fun getCommunityById(communityId: String): CommunityGroup?

    // Events
    suspend fun getEventsByGroupId(groupId: String): List<Event>
    suspend fun getEventById(eventId: String): Event?

    // RSVP
    suspend fun rsvpToEvent(userId: String, eventId: String): Rsvp
    suspend fun getUserRsvps(userId: String): List<Rsvp>
    suspend fun hasUserRsvped(userId: String, eventId: String): Boolean

    // Attendance
    suspend fun checkInToEvent(userId: String, eventId: String): Attendance
    suspend fun hasUserCheckedIn(userId: String, eventId: String): Boolean
}