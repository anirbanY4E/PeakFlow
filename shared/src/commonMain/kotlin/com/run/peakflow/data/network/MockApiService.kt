package com.run.peakflow.data.network

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.models.User
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Clock

class MockApiService : ApiService {

    private val users = mutableListOf<User>()
    private val rsvps = mutableListOf<Rsvp>()
    private val attendances = mutableListOf<Attendance>()

    private val communities = listOf(
        CommunityGroup(
            id = "grp_001",
            title = "Cubbon Park Morning Runners",
            description = "Daily 5K runs at 6 AM. All fitness levels welcome. We meet at the bandstand and finish with stretches near the library.",
            category = "Running",
            city = "Bangalore",
            memberCount = 156,
            createdBy = "user_001"
        ),
        CommunityGroup(
            id = "grp_002",
            title = "Indiranagar Calisthenics Crew",
            description = "Bodyweight fitness enthusiasts. We train at the outdoor gym near 12th Main. Bring your own mat.",
            category = "Calisthenics",
            city = "Bangalore",
            memberCount = 89,
            createdBy = "user_002"
        ),
        CommunityGroup(
            id = "grp_003",
            title = "HSR Weekend Trekkers",
            description = "Weekend treks around Bangalore. Nandi Hills, Skandagiri, Savandurga and more. Carpooling available.",
            category = "Trekking",
            city = "Bangalore",
            memberCount = 234,
            createdBy = "user_003"
        ),
        CommunityGroup(
            id = "grp_004",
            title = "Koramangala Cycling Club",
            description = "Road cycling group. Weekend long rides and weekday evening short loops. Minimum requirement: hybrid or road bike.",
            category = "Cycling",
            city = "Bangalore",
            memberCount = 178,
            createdBy = "user_004"
        ),
        CommunityGroup(
            id = "grp_005",
            title = "Ulsoor Lake Kayaking Group",
            description = "Kayaking sessions every Saturday morning. Equipment provided. Beginners welcome.",
            category = "Kayaking",
            city = "Bangalore",
            memberCount = 45,
            createdBy = "user_005"
        ),
        CommunityGroup(
            id = "grp_006",
            title = "Whitefield Yoga Circle",
            description = "Outdoor yoga sessions in EPIP Zone park. Hatha and Vinyasa flow. Bring your own mat.",
            category = "Yoga",
            city = "Bangalore",
            memberCount = 112,
            createdBy = "user_006"
        )
    )

    private val events = listOf(
        // Cubbon Park Morning Runners events
        Event(
            id = "evt_001",
            groupId = "grp_001",
            title = "Sunday Long Run - 10K",
            date = "2025-01-12",
            time = "06:00 AM",
            location = "Cubbon Park Bandstand, MG Road",
            description = "Weekly long run. We'll do 2 loops of the park. Water available at midpoint.",
            maxParticipants = 50,
            currentParticipants = 32
        ),
        Event(
            id = "evt_002",
            groupId = "grp_001",
            title = "Weekday Easy Run - 5K",
            date = "2025-01-14",
            time = "06:00 AM",
            location = "Cubbon Park Bandstand, MG Road",
            description = "Easy pace run. Perfect for beginners or recovery day.",
            maxParticipants = 40,
            currentParticipants = 18
        ),
        // Indiranagar Calisthenics events
        Event(
            id = "evt_003",
            groupId = "grp_002",
            title = "Pull-up Challenge Session",
            date = "2025-01-13",
            time = "07:00 AM",
            location = "Outdoor Gym, 12th Main Indiranagar",
            description = "Monthly pull-up challenge. Categories for beginners and advanced.",
            maxParticipants = 30,
            currentParticipants = 24
        ),
        Event(
            id = "evt_004",
            groupId = "grp_002",
            title = "Core & Mobility Workshop",
            date = "2025-01-15",
            time = "06:30 AM",
            location = "Outdoor Gym, 12th Main Indiranagar",
            description = "Focus on core strength and flexibility. Instructor-led session.",
            maxParticipants = 25,
            currentParticipants = 12
        ),
        // HSR Weekend Trekkers events
        Event(
            id = "evt_005",
            groupId = "grp_003",
            title = "Skandagiri Night Trek",
            date = "2025-01-18",
            time = "02:00 AM",
            location = "HSR BDA Complex (Carpool pickup)",
            description = "Night trek to catch sunrise at summit. Moderate difficulty. Headlamp required.",
            maxParticipants = 35,
            currentParticipants = 35
        ),
        Event(
            id = "evt_006",
            groupId = "grp_003",
            title = "Nandi Hills Sunrise",
            date = "2025-01-19",
            time = "04:00 AM",
            location = "HSR BDA Complex (Carpool pickup)",
            description = "Easy trek. Good for beginners. Breakfast at hilltop.",
            maxParticipants = 40,
            currentParticipants = 28
        ),
        // Koramangala Cycling Club events
        Event(
            id = "evt_007",
            groupId = "grp_004",
            title = "Weekend Century Ride",
            date = "2025-01-12",
            time = "05:00 AM",
            location = "Sony World Junction, Koramangala",
            description = "100km ride to Nandi Hills and back. Average speed 25kmph. Carry enough water.",
            maxParticipants = 20,
            currentParticipants = 16
        ),
        // Ulsoor Lake Kayaking events
        Event(
            id = "evt_008",
            groupId = "grp_005",
            title = "Saturday Morning Paddle",
            date = "2025-01-11",
            time = "07:00 AM",
            location = "Ulsoor Lake Boating Point",
            description = "Relaxed 1-hour kayaking session. Equipment provided. Beginners welcome.",
            maxParticipants = 15,
            currentParticipants = 9
        ),
        // Whitefield Yoga Circle events
        Event(
            id = "evt_009",
            groupId = "grp_006",
            title = "Sunday Sunrise Yoga",
            date = "2025-01-12",
            time = "06:30 AM",
            location = "EPIP Zone Park, Whitefield",
            description = "75-minute Vinyasa flow session. All levels welcome.",
            maxParticipants = 30,
            currentParticipants = 22
        )
    )

    override suspend fun createUser(name: String, city: String): User {
        delay(300)
        val user = User(
            id = "user_${Random.nextInt(1000, 9999)}",
            name = name,
            city = city
        )
        users.add(user)
        return user
    }

    override suspend fun getUser(userId: String): User? {
        delay(200)
        return users.find { it.id == userId }
    }

    override suspend fun getCommunities(): List<CommunityGroup> {
        delay(400)
        return communities
    }

    override suspend fun getCommunitiesByCity(city: String): List<CommunityGroup> {
        delay(400)
        return communities.filter { it.city.equals(city, ignoreCase = true) }
    }

    override suspend fun getCommunityById(communityId: String): CommunityGroup? {
        delay(300)
        return communities.find { it.id == communityId }
    }

    override suspend fun getEventsByGroupId(groupId: String): List<Event> {
        delay(350)
        return events.filter { it.groupId == groupId }
    }

    override suspend fun getEventById(eventId: String): Event? {
        delay(300)
        return events.find { it.id == eventId }
    }

    override suspend fun rsvpToEvent(userId: String, eventId: String): Rsvp {
        delay(400)
        val rsvp = Rsvp(
            id = "rsvp_${Random.nextInt(1000, 9999)}",
            userId = userId,
            eventId = eventId,
            timestamp = Random.nextLong(1704067200000, 1735689600000)
        )
        rsvps.add(rsvp)
        return rsvp
    }

    override suspend fun checkInToEvent(userId: String, eventId: String): Attendance {
        delay(400)
        val attendance = Attendance(
            id = "att_${Random.nextInt(1000, 9999)}",
            eventId = eventId,
            userId = userId,
            checkInTimestamp = Random.nextLong(1704067200000, 1735689600000)
        )
        attendances.add(attendance)
        return attendance
    }
    override suspend fun getUserRsvps(userId: String): List<Rsvp> {
        delay(300)
        return rsvps.filter { it.userId == userId }
    }

    override suspend fun hasUserRsvped(userId: String, eventId: String): Boolean {
        delay(200)
        return rsvps.any { it.userId == userId && it.eventId == eventId }
    }

    override suspend fun hasUserCheckedIn(userId: String, eventId: String): Boolean {
        delay(200)
        return attendances.any { it.userId == userId && it.eventId == eventId }
    }
}