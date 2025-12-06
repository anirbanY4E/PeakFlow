package com.run.peakflow.data.network

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.models.Post
import com.run.peakflow.data.models.PostComment
import com.run.peakflow.data.models.RequestStatus
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.models.User
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockApiService : ApiService {

    // ==================== IN-MEMORY DATA STORES ====================

    private val users = mutableListOf<User>()
    private val memberships = mutableListOf<CommunityMembership>()
    private val joinRequests = mutableListOf<JoinRequest>()
    private val inviteCodes = mutableListOf<InviteCode>()
    private val posts = mutableListOf<Post>()
    private val postLikes = mutableMapOf<String, MutableSet<String>>() // postId -> Set<userId>
    private val comments = mutableListOf<PostComment>()
    private val rsvps = mutableListOf<Rsvp>()
    private val attendances = mutableListOf<Attendance>()

    // ==================== SEED DATA ====================

    private val communities = listOf(
        CommunityGroup(
            id = "grp_001",
            title = "Cubbon Park Morning Runners",
            description = "Daily 5K runs at 6 AM. All fitness levels welcome. We meet at the bandstand and finish with stretches near the library.",
            category = EventCategory.RUNNING,
            city = "Bangalore",
            memberCount = 156,
            createdBy = "user_admin_001",
            imageUrl = "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=400",
            coverUrl = "https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=800",
            rules = listOf(
                "Be respectful to all members",
                "Arrive 5 minutes before start time",
                "No headphones during group runs",
                "Support slower runners"
            ),
            createdAt = 1610000000000
        ),
        CommunityGroup(
            id = "grp_002",
            title = "Indiranagar Calisthenics Crew",
            description = "Bodyweight fitness enthusiasts. We train at the outdoor gym near 12th Main. Bring your own mat.",
            category = EventCategory.CALISTHENICS,
            city = "Bangalore",
            memberCount = 89,
            createdBy = "user_admin_002",
            imageUrl = "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=400",
            coverUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
            rules = listOf(
                "Warm up before exercises",
                "Clean equipment after use",
                "Help beginners with form"
            ),
            createdAt = 1615000000000
        ),
        CommunityGroup(
            id = "grp_003",
            title = "HSR Weekend Trekkers",
            description = "Weekend treks around Bangalore. Nandi Hills, Skandagiri, Savandurga and more. Carpooling available.",
            category = EventCategory.TREKKING,
            city = "Bangalore",
            memberCount = 234,
            createdBy = "user_admin_003",
            imageUrl = "https://images.unsplash.com/photo-1551632811-561732d1e306?w=400",
            coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800",
            rules = listOf(
                "Carry enough water",
                "No littering on trails",
                "Follow trek leader instructions",
                "Inform if you need to drop out"
            ),
            createdAt = 1620000000000
        ),
        CommunityGroup(
            id = "grp_004",
            title = "Koramangala Cycling Club",
            description = "Road cycling group. Weekend long rides and weekday evening short loops. Minimum requirement: hybrid or road bike.",
            category = EventCategory.CYCLING,
            city = "Bangalore",
            memberCount = 178,
            createdBy = "user_admin_004",
            imageUrl = "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=400",
            coverUrl = "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=800",
            rules = listOf(
                "Helmet mandatory",
                "Maintain safe distance",
                "Signal before turns",
                "Carry basic repair kit"
            ),
            createdAt = 1625000000000
        ),
        CommunityGroup(
            id = "grp_005",
            title = "Ulsoor Lake Kayaking Group",
            description = "Kayaking sessions every Saturday morning. Equipment provided. Beginners welcome.",
            category = EventCategory.KAYAKING,
            city = "Bangalore",
            memberCount = 45,
            createdBy = "user_admin_005",
            imageUrl = "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=400",
            coverUrl = "https://images.unsplash.com/photo-1572552840353-983e4f067e97?w=800",
            rules = listOf(
                "Life jacket mandatory",
                "No swimming during sessions",
                "Follow instructor guidance"
            ),
            createdAt = 1630000000000
        ),
        CommunityGroup(
            id = "grp_006",
            title = "Whitefield Yoga Circle",
            description = "Outdoor yoga sessions in EPIP Zone park. Hatha and Vinyasa flow. Bring your own mat.",
            category = EventCategory.YOGA,
            city = "Bangalore",
            memberCount = 112,
            createdBy = "user_admin_006",
            imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400",
            coverUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800",
            rules = listOf(
                "Arrive 10 minutes early",
                "Maintain silence during practice",
                "Bring your own mat and water"
            ),
            createdAt = 1635000000000
        )
    )

    private val events = mutableListOf(
        Event(
            id = "evt_001",
            groupId = "grp_001",
            title = "Sunday Long Run - 10K",
            description = "Weekly long run. We'll do 2 loops of the park. Water available at midpoint.",
            category = EventCategory.RUNNING,
            date = "2025-01-12",
            time = "06:00 AM",
            endTime = "08:00 AM",
            location = "Cubbon Park Bandstand, MG Road",
            latitude = 12.9763,
            longitude = 77.5929,
            imageUrl = "https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=600",
            maxParticipants = 50,
            currentParticipants = 32,
            isFree = true,
            price = null,
            createdAt = 1704067200000
        ),
        Event(
            id = "evt_002",
            groupId = "grp_001",
            title = "Weekday Easy Run - 5K",
            description = "Easy pace run. Perfect for beginners or recovery day.",
            category = EventCategory.RUNNING,
            date = "2025-01-14",
            time = "06:00 AM",
            endTime = "07:00 AM",
            location = "Cubbon Park Bandstand, MG Road",
            latitude = 12.9763,
            longitude = 77.5929,
            imageUrl = null,
            maxParticipants = 40,
            currentParticipants = 18,
            isFree = true,
            price = null,
            createdAt = 1704153600000
        ),
        Event(
            id = "evt_003",
            groupId = "grp_002",
            title = "Pull-up Challenge Session",
            description = "Monthly pull-up challenge. Categories for beginners and advanced.",
            category = EventCategory.CALISTHENICS,
            date = "2025-01-13",
            time = "07:00 AM",
            endTime = "09:00 AM",
            location = "Outdoor Gym, 12th Main Indiranagar",
            latitude = 12.9784,
            longitude = 77.6408,
            imageUrl = "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=600",
            maxParticipants = 30,
            currentParticipants = 24,
            isFree = true,
            price = null,
            createdAt = 1704240000000
        ),
        Event(
            id = "evt_004",
            groupId = "grp_002",
            title = "Core & Mobility Workshop",
            description = "Focus on core strength and flexibility. Instructor-led session.",
            category = EventCategory.CALISTHENICS,
            date = "2025-01-15",
            time = "06:30 AM",
            endTime = "08:00 AM",
            location = "Outdoor Gym, 12th Main Indiranagar",
            latitude = 12.9784,
            longitude = 77.6408,
            imageUrl = null,
            maxParticipants = 25,
            currentParticipants = 12,
            isFree = true,
            price = null,
            createdAt = 1704326400000
        ),
        Event(
            id = "evt_005",
            groupId = "grp_003",
            title = "Skandagiri Night Trek",
            description = "Night trek to catch sunrise at summit. Moderate difficulty. Headlamp required.",
            category = EventCategory.TREKKING,
            date = "2025-01-18",
            time = "02:00 AM",
            endTime = "10:00 AM",
            location = "HSR BDA Complex (Carpool pickup)",
            latitude = 12.9116,
            longitude = 77.6389,
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600",
            maxParticipants = 35,
            currentParticipants = 35,
            isFree = false,
            price = 500.0,
            createdAt = 1704412800000
        ),
        Event(
            id = "evt_006",
            groupId = "grp_003",
            title = "Nandi Hills Sunrise",
            description = "Easy trek. Good for beginners. Breakfast at hilltop.",
            category = EventCategory.TREKKING,
            date = "2025-01-19",
            time = "04:00 AM",
            endTime = "11:00 AM",
            location = "HSR BDA Complex (Carpool pickup)",
            latitude = 12.9116,
            longitude = 77.6389,
            imageUrl = "https://images.unsplash.com/photo-1551632811-561732d1e306?w=600",
            maxParticipants = 40,
            currentParticipants = 28,
            isFree = false,
            price = 350.0,
            createdAt = 1704499200000
        ),
        Event(
            id = "evt_007",
            groupId = "grp_004",
            title = "Weekend Century Ride",
            description = "100km ride to Nandi Hills and back. Average speed 25kmph. Carry enough water.",
            category = EventCategory.CYCLING,
            date = "2025-01-12",
            time = "05:00 AM",
            endTime = "11:00 AM",
            location = "Sony World Junction, Koramangala",
            latitude = 12.9352,
            longitude = 77.6245,
            imageUrl = "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=600",
            maxParticipants = 20,
            currentParticipants = 16,
            isFree = true,
            price = null,
            createdAt = 1704585600000
        ),
        Event(
            id = "evt_008",
            groupId = "grp_005",
            title = "Saturday Morning Paddle",
            description = "Relaxed 1-hour kayaking session. Equipment provided. Beginners welcome.",
            category = EventCategory.KAYAKING,
            date = "2025-01-11",
            time = "07:00 AM",
            endTime = "09:00 AM",
            location = "Ulsoor Lake Boating Point",
            latitude = 12.9825,
            longitude = 77.6200,
            imageUrl = "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=600",
            maxParticipants = 15,
            currentParticipants = 9,
            isFree = false,
            price = 200.0,
            createdAt = 1704672000000
        ),
        Event(
            id = "evt_009",
            groupId = "grp_006",
            title = "Sunday Sunrise Yoga",
            description = "75-minute Vinyasa flow session. All levels welcome.",
            category = EventCategory.YOGA,
            date = "2025-01-12",
            time = "06:30 AM",
            endTime = "07:45 AM",
            location = "EPIP Zone Park, Whitefield",
            latitude = 12.9698,
            longitude = 77.7500,
            imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=600",
            maxParticipants = 30,
            currentParticipants = 22,
            isFree = true,
            price = null,
            createdAt = 1704758400000
        )
    )

    private val seedPosts = listOf(
        Post(
            id = "post_001",
            communityId = "grp_001",
            authorId = "user_admin_001",
            authorName = "Rahul (Admin)",
            authorAvatarUrl = null,
            content = "Great turnout for today's morning run! 🏃‍♂️ 45 people showed up despite the rain. Special thanks to everyone who made it. See you all on Sunday for the long run!",
            imageUrl = "https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=600",
            likesCount = 24,
            commentsCount = 8,
            createdAt = 1704700800000
        ),
        Post(
            id = "post_002",
            communityId = "grp_001",
            authorId = "user_admin_001",
            authorName = "Rahul (Admin)",
            authorAvatarUrl = null,
            content = "Sunday long run route map attached. We'll start from the bandstand, do 2 loops, and finish near the library. Water station at the halfway point. See you at 6 AM sharp!",
            imageUrl = "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=600",
            likesCount = 38,
            commentsCount = 12,
            createdAt = 1704614400000
        ),
        Post(
            id = "post_003",
            communityId = "grp_003",
            authorId = "user_admin_003",
            authorName = "Priya (Admin)",
            authorAvatarUrl = null,
            content = "🏔️ Registrations open for Skandagiri night trek! Limited to 35 people. This will fill up fast - register now to secure your spot. Link in the events section.",
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600",
            likesCount = 42,
            commentsCount = 15,
            createdAt = 1704528000000
        ),
        Post(
            id = "post_004",
            communityId = "grp_002",
            authorId = "user_admin_002",
            authorName = "Amit (Admin)",
            authorAvatarUrl = null,
            content = "Monthly challenge results are in! 💪 Congratulations to Vikram for hitting 25 consecutive pull-ups. New record for our community! Full leaderboard in comments.",
            imageUrl = null,
            likesCount = 56,
            commentsCount = 23,
            createdAt = 1704441600000
        ),
        Post(
            id = "post_005",
            communityId = "grp_004",
            authorId = "user_admin_004",
            authorName = "Sneha (Admin)",
            authorAvatarUrl = null,
            content = "Century ride this weekend! 🚴 Route: Koramangala → Nandi Hills → Back. 100km total. Average pace 25kmph. Make sure your bike is serviced. Helmet mandatory!",
            imageUrl = "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=600",
            likesCount = 31,
            commentsCount = 9,
            createdAt = 1704355200000
        )
    )

    init {
        // Initialize seed posts
        posts.addAll(seedPosts)

        // Initialize pre-seeded admin user
        users.add(
            User(
                id = "user_admin_001",
                name = "Admin User",
                email = "admin@test.com",
                phone = null,
                city = "Bangalore",
                avatarUrl = null,
                interests = listOf(EventCategory.RUNNING, EventCategory.TREKKING),
                createdAt = 1704067200000,
                isVerified = true
            )
        )

        // Initialize admin membership for Cubbon Park Runners
        memberships.add(
            CommunityMembership(
                id = "mem_admin_001",
                userId = "user_admin_001",
                communityId = "grp_001", // Cubbon Park Morning Runners
                role = MembershipRole.ADMIN,
                joinedAt = 1704067200000 // Jan 1, 2024
            )
        )

        // Initialize seed invite codes
        inviteCodes.addAll(listOf(
            InviteCode(
                id = "inv_001",
                code = "CUBBON-RUNNERS-7X9K",
                communityId = "grp_001",
                createdBy = "user_admin_001",
                maxUses = null,
                currentUses = 12,
                expiresAt = null,
                isActive = true,
                createdAt = 1704067200000
            ),
            InviteCode(
                id = "inv_002",
                code = "HSR-TREK-2025",
                communityId = "grp_003",
                createdBy = "user_admin_003",
                maxUses = 50,
                currentUses = 23,
                expiresAt = 1735689600000,
                isActive = true,
                createdAt = 1704153600000
            ),
            InviteCode(
                id = "inv_003",
                code = "CALI-CREW-JOIN",
                communityId = "grp_002",
                createdBy = "user_admin_002",
                maxUses = null,
                currentUses = 8,
                expiresAt = null,
                isActive = true,
                createdAt = 1704240000000
            )
        ))
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun generateId(prefix: String): String {
        return "${prefix}_${Random.nextInt(100000, 999999)}"
    }

    private fun currentTimestamp(): Long {
        return Random.nextLong(1704067200000, 1735689600000)
    }

    // ==================== AUTH ====================

    override suspend fun signUp(email: String?, phone: String?, password: String): User {
        delay(500)
        val user = User(
            id = generateId("user"),
            name = "",
            email = email,
            phone = phone,
            city = "Bangalore",
            avatarUrl = null,
            interests = emptyList(),
            createdAt = currentTimestamp(),
            isVerified = false
        )
        users.add(user)
        return user
    }

    override suspend fun signIn(emailOrPhone: String, password: String): User? {
        delay(500)
        return users.find {
            it.email == emailOrPhone || it.phone == emailOrPhone
        }
    }

    override suspend fun verifyOtp(userId: String, otp: String): Boolean {
        delay(400)
        // Mock: accept any 6-digit OTP
        if (otp.length == 6 && otp.all { it.isDigit() }) {
            val userIndex = users.indexOfFirst { it.id == userId }
            if (userIndex != -1) {
                users[userIndex] = users[userIndex].copy(isVerified = true)
                return true
            }
        }
        return false
    }

    override suspend fun resendOtp(userId: String): Boolean {
        delay(300)
        return users.any { it.id == userId }
    }

    // ==================== USER ====================

    override suspend fun getUser(userId: String): User? {
        delay(200)
        return users.find { it.id == userId }
    }

    override suspend fun updateUser(user: User): User {
        delay(300)
        val index = users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            users[index] = user
        }
        return user
    }

    override suspend fun completeProfile(
        userId: String,
        name: String,
        city: String,
        interests: List<EventCategory>,
        avatarUrl: String?
    ): User {
        delay(400)
        val index = users.indexOfFirst { it.id == userId }
        if (index != -1) {
            val updatedUser = users[index].copy(
                name = name,
                city = city,
                interests = interests,
                avatarUrl = avatarUrl
            )
            users[index] = updatedUser
            return updatedUser
        }
        throw Exception("User not found")
    }

    // ==================== INVITE CODES ====================

    override suspend fun validateInviteCode(code: String): InviteCode? {
        delay(300)
        val invite = inviteCodes.find {
            it.code.equals(code, ignoreCase = true) && it.isActive
        }

        if (invite != null) {
            // Check expiry
            if (invite.expiresAt != null && invite.expiresAt < currentTimestamp()) {
                return null
            }
            // Check max uses
            if (invite.maxUses != null && invite.currentUses >= invite.maxUses) {
                return null
            }
        }

        return invite
    }

    override suspend fun useInviteCode(code: String, userId: String): CommunityMembership {
        delay(400)
        val invite = validateInviteCode(code)
            ?: throw Exception("Invalid or expired invite code")

        // Update invite usage
        val inviteIndex = inviteCodes.indexOfFirst { it.code.equals(code, ignoreCase = true) }
        if (inviteIndex != -1) {
            inviteCodes[inviteIndex] = inviteCodes[inviteIndex].copy(
                currentUses = inviteCodes[inviteIndex].currentUses + 1
            )
        }

        // Create membership
        val membership = CommunityMembership(
            id = generateId("mem"),
            userId = userId,
            communityId = invite.communityId,
            role = MembershipRole.MEMBER,
            joinedAt = currentTimestamp(),
            invitedBy = invite.createdBy
        )
        memberships.add(membership)

        return membership
    }

    override suspend fun generateInviteCode(
        communityId: String,
        createdBy: String,
        maxUses: Int?,
        expiresInDays: Int?
    ): InviteCode {
        delay(400)
        val community = communities.find { it.id == communityId }
            ?: throw Exception("Community not found")

        val codePrefix = community.title
            .split(" ")
            .take(2)
            .joinToString("-") { it.uppercase().take(6) }
        val codeSuffix = Random.nextInt(1000, 9999).toString()

        val invite = InviteCode(
            id = generateId("inv"),
            code = "$codePrefix-$codeSuffix",
            communityId = communityId,
            createdBy = createdBy,
            maxUses = maxUses,
            currentUses = 0,
            expiresAt = if (expiresInDays != null) {
                currentTimestamp() + (expiresInDays * 24 * 60 * 60 * 1000L)
            } else null,
            isActive = true,
            createdAt = currentTimestamp()
        )
        inviteCodes.add(invite)

        return invite
    }

    override suspend fun getUserInviteCodes(userId: String, communityId: String): List<InviteCode> {
        delay(300)
        return inviteCodes.filter {
            it.createdBy == userId && it.communityId == communityId && it.isActive
        }
    }

    // ==================== COMMUNITIES ====================

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

    override suspend fun getDiscoverCommunities(
        city: String,
        excludeUserCommunities: List<String>
    ): List<CommunityGroup> {
        delay(400)
        return communities.filter {
            it.city.equals(city, ignoreCase = true) &&
                    it.id !in excludeUserCommunities
        }
    }

    // ==================== MEMBERSHIPS ====================

    override suspend fun getUserMemberships(userId: String): List<CommunityMembership> {
        delay(300)
        return memberships.filter { it.userId == userId && it.role != MembershipRole.PENDING }
    }

    override suspend fun getCommunityMemberships(communityId: String): List<CommunityMembership> {
        delay(300)
        return memberships.filter {
            it.communityId == communityId && it.role != MembershipRole.PENDING
        }
    }

    override suspend fun getMembershipRole(userId: String, communityId: String): CommunityMembership? {
        delay(200)
        return memberships.find { it.userId == userId && it.communityId == communityId }
    }

    override suspend fun isUserMemberOf(userId: String, communityId: String): Boolean {
        delay(200)
        return memberships.any {
            it.userId == userId &&
                    it.communityId == communityId &&
                    it.role != MembershipRole.PENDING
        }
    }

    // ==================== JOIN REQUESTS ====================

    override suspend fun requestToJoin(userId: String, communityId: String): JoinRequest {
        delay(400)
        val request = JoinRequest(
            id = generateId("req"),
            userId = userId,
            communityId = communityId,
            status = RequestStatus.PENDING,
            requestedAt = currentTimestamp(),
            reviewedAt = null,
            reviewedBy = null
        )
        joinRequests.add(request)
        return request
    }

    override suspend fun getPendingJoinRequests(communityId: String): List<JoinRequest> {
        delay(300)
        return joinRequests.filter {
            it.communityId == communityId && it.status == RequestStatus.PENDING
        }
    }

    override suspend fun getUserJoinRequests(userId: String): List<JoinRequest> {
        delay(300)
        return joinRequests.filter { it.userId == userId }
    }

    override suspend fun approveJoinRequest(
        requestId: String,
        reviewedBy: String
    ): CommunityMembership {
        delay(400)
        val requestIndex = joinRequests.indexOfFirst { it.id == requestId }
        if (requestIndex == -1) throw Exception("Request not found")

        val request = joinRequests[requestIndex]
        joinRequests[requestIndex] = request.copy(
            status = RequestStatus.APPROVED,
            reviewedAt = currentTimestamp(),
            reviewedBy = reviewedBy
        )

        val membership = CommunityMembership(
            id = generateId("mem"),
            userId = request.userId,
            communityId = request.communityId,
            role = MembershipRole.MEMBER,
            joinedAt = currentTimestamp(),
            invitedBy = reviewedBy
        )
        memberships.add(membership)

        return membership
    }

    override suspend fun rejectJoinRequest(
        requestId: String,
        reviewedBy: String
    ): JoinRequest {
        delay(400)
        val requestIndex = joinRequests.indexOfFirst { it.id == requestId }
        if (requestIndex == -1) throw Exception("Request not found")

        val request = joinRequests[requestIndex]
        val updated = request.copy(
            status = RequestStatus.REJECTED,
            reviewedAt = currentTimestamp(),
            reviewedBy = reviewedBy
        )
        joinRequests[requestIndex] = updated

        return updated
    }

    override suspend fun hasUserRequestedToJoin(userId: String, communityId: String): Boolean {
        delay(200)
        return joinRequests.any {
            it.userId == userId &&
                    it.communityId == communityId &&
                    it.status == RequestStatus.PENDING
        }
    }

    // ==================== POSTS ====================

    override suspend fun getCommunityPosts(communityId: String): List<Post> {
        delay(400)
        return posts
            .filter { it.communityId == communityId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getFeedPosts(communityIds: List<String>): List<Post> {
        delay(400)
        return posts
            .filter { it.communityId in communityIds }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getPostById(postId: String): Post? {
        delay(300)
        return posts.find { it.id == postId }
    }

    override suspend fun createPost(
        communityId: String,
        authorId: String,
        content: String,
        imageUrl: String?
    ): Post {
        delay(500)
        val user = users.find { it.id == authorId }
        val post = Post(
            id = generateId("post"),
            communityId = communityId,
            authorId = authorId,
            authorName = user?.name ?: "Unknown",
            authorAvatarUrl = user?.avatarUrl,
            content = content,
            imageUrl = imageUrl,
            likesCount = 0,
            commentsCount = 0,
            createdAt = currentTimestamp()
        )
        posts.add(0, post)
        return post
    }

    override suspend fun deletePost(postId: String): Boolean {
        delay(300)
        return posts.removeAll { it.id == postId }
    }

    override suspend fun likePost(postId: String, userId: String): Boolean {
        delay(200)
        val likes = postLikes.getOrPut(postId) { mutableSetOf() }
        if (userId !in likes) {
            likes.add(userId)
            val postIndex = posts.indexOfFirst { it.id == postId }
            if (postIndex != -1) {
                posts[postIndex] = posts[postIndex].copy(
                    likesCount = posts[postIndex].likesCount + 1
                )
            }
        }
        return true
    }

    override suspend fun unlikePost(postId: String, userId: String): Boolean {
        delay(200)
        val likes = postLikes[postId] ?: return false
        if (userId in likes) {
            likes.remove(userId)
            val postIndex = posts.indexOfFirst { it.id == postId }
            if (postIndex != -1) {
                posts[postIndex] = posts[postIndex].copy(
                    likesCount = (posts[postIndex].likesCount - 1).coerceAtLeast(0)
                )
            }
        }
        return true
    }

    override suspend fun hasUserLikedPost(postId: String, userId: String): Boolean {
        delay(150)
        return postLikes[postId]?.contains(userId) == true
    }

    // ==================== COMMENTS ====================

    override suspend fun getPostComments(postId: String): List<PostComment> {
        delay(300)
        return comments
            .filter { it.postId == postId }
            .sortedBy { it.createdAt }
    }

    override suspend fun addComment(
        postId: String,
        userId: String,
        content: String
    ): PostComment {
        delay(400)
        val user = users.find { it.id == userId }
        val comment = PostComment(
            id = generateId("cmt"),
            postId = postId,
            userId = userId,
            userName = user?.name ?: "Unknown",
            userAvatarUrl = user?.avatarUrl,
            content = content,
            likesCount = 0,
            createdAt = currentTimestamp()
        )
        comments.add(comment)

        // Update post-comments count
        val postIndex = posts.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            posts[postIndex] = posts[postIndex].copy(
                commentsCount = posts[postIndex].commentsCount + 1
            )
        }

        return comment
    }

    override suspend fun deleteComment(commentId: String): Boolean {
        delay(300)
        val comment = comments.find { it.id == commentId }
        if (comment != null) {
            comments.remove(comment)
            val postIndex = posts.indexOfFirst { it.id == comment.postId }
            if (postIndex != -1) {
                posts[postIndex] = posts[postIndex].copy(
                    commentsCount = (posts[postIndex].commentsCount - 1).coerceAtLeast(0)
                )
            }
            return true
        }
        return false
    }

    // ==================== EVENTS ====================

    override suspend fun createEvent(
        communityId: String,
        title: String,
        description: String,
        category: EventCategory,
        date: String,
        time: String,
        location: String,
        maxParticipants: Int,
        isFree: Boolean,
        price: Double?
    ): Event {
        delay(500)

        val event = Event(
            id = generateId("event"),
            groupId = communityId,
            title = title,
            description = description,
            category = category,
            date = date,
            time = time,
            endTime = null,
            location = location,
            latitude = null,
            longitude = null,
            imageUrl = null,
            maxParticipants = maxParticipants,
            currentParticipants = 0,
            isFree = isFree,
            price = price,
            createdAt = currentTimestamp()
        )

        events.add(event)
        return event
    }

    override suspend fun getEventsByGroupId(groupId: String): List<Event> {
        delay(350)
        return events
            .filter { it.groupId == groupId }
            .sortedBy { it.date }
    }

    override suspend fun getEventById(eventId: String): Event? {
        delay(300)
        return events.find { it.id == eventId }
    }

    override suspend fun getNearbyEvents(
        city: String,
        category: EventCategory?,
        communityIds: List<String>
    ): List<Event> {
        delay(400)
        return events.filter { event ->
            val community = communities.find { it.id == event.groupId }
            val inCity = community?.city?.equals(city, ignoreCase = true) == true
            val inCommunity = event.groupId in communityIds
            val matchesCategory = category == null || event.category == category

            inCity && inCommunity && matchesCategory
        }.sortedBy { it.date }
    }

    override suspend fun getAllAccessibleEvents(communityIds: List<String>): List<Event> {
        delay(400)
        return events
            .filter { it.groupId in communityIds }
            .sortedBy { it.date }
    }

    // ==================== RSVP ====================

    override suspend fun rsvpToEvent(userId: String, eventId: String): Rsvp {
        delay(400)
        val rsvp = Rsvp(
            id = generateId("rsvp"),
            userId = userId,
            eventId = eventId,
            timestamp = currentTimestamp()
        )
        rsvps.add(rsvp)

        // Update event participant count
        val eventIndex = events.indexOfFirst { it.id == eventId }
        if (eventIndex != -1) {
            events[eventIndex] = events[eventIndex].copy(
                currentParticipants = events[eventIndex].currentParticipants + 1
            )
        }

        return rsvp
    }

    override suspend fun cancelRsvp(userId: String, eventId: String): Boolean {
        delay(300)
        val removed = rsvps.removeAll { it.userId == userId && it.eventId == eventId }
        if (removed) {
            val eventIndex = events.indexOfFirst { it.id == eventId }
            if (eventIndex != -1) {
                events[eventIndex] = events[eventIndex].copy(
                    currentParticipants = (events[eventIndex].currentParticipants - 1).coerceAtLeast(0)
                )
            }
        }
        return removed
    }

    override suspend fun getUserRsvps(userId: String): List<Rsvp> {
        delay(300)
        return rsvps.filter { it.userId == userId }
    }

    override suspend fun getEventRsvps(eventId: String): List<Rsvp> {
        delay(300)
        return rsvps.filter { it.eventId == eventId }
    }

    override suspend fun hasUserRsvped(userId: String, eventId: String): Boolean {
        delay(200)
        return rsvps.any { it.userId == userId && it.eventId == eventId }
    }

    // ==================== ATTENDANCE ====================

    override suspend fun checkInToEvent(userId: String, eventId: String): Attendance {
        delay(400)
        val attendance = Attendance(
            id = generateId("att"),
            eventId = eventId,
            userId = userId,
            checkInTimestamp = currentTimestamp()
        )
        attendances.add(attendance)
        return attendance
    }

    override suspend fun hasUserCheckedIn(userId: String, eventId: String): Boolean {
        delay(200)
        return attendances.any { it.userId == userId && it.eventId == eventId }
    }

    override suspend fun getEventAttendance(eventId: String): List<Attendance> {
        delay(300)
        return attendances.filter { it.eventId == eventId }
    }

    override suspend fun getUserAttendanceHistory(userId: String): List<Attendance> {
        delay(300)
        return attendances.filter { it.userId == userId }
    }
}