package com.run.peakflow.data.network

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.models.Post
import com.run.peakflow.data.models.PostComment
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.models.User
import kotlinx.coroutines.flow.Flow

/**
 * Combined result from the get_event_detail RPC.
 * Contains the event, community info, RSVP status, and check-in status in one shot.
 */
data class EventDetailResult(
    val event: Event,
    val communityName: String,
    val communityImage: String?,
    val hasRsvped: Boolean,
    val hasCheckedIn: Boolean
)

/**
 * Represents a community member with their profile info, returned by batch RPC.
 */
data class CommunityMemberWithProfile(
    val membership: CommunityMembership,
    val userName: String,
    val userEmail: String?,
    val userAvatarUrl: String?
)

/**
 * Represents the authentication session status
 */
sealed interface AuthSessionStatus {
    data class Authenticated(val userId: String) : AuthSessionStatus
    object Loading : AuthSessionStatus
    object NotAuthenticated : AuthSessionStatus
    object NetworkError : AuthSessionStatus
}

interface ApiService {

    // ==================== AUTH ====================

    suspend fun signUp(
        email: String?,
        phone: String?,
        password: String
    ): User

    suspend fun signIn(
        emailOrPhone: String,
        password: String
    ): User?

    suspend fun signInWithGoogle(
        idToken: String,
        email: String,
        displayName: String?,
        photoUrl: String?
    ): User

    suspend fun startGoogleBrowserOAuth()

    suspend fun verifyOtp(
        userId: String,
        otp: String
    ): Boolean

    suspend fun resendOtp(userId: String): Boolean

    /**
     * Get the current session user ID synchronously.
     * May return null if session is still loading.
     */
    suspend fun getSessionUserId(): String?

    /**
     * Observe authentication session status changes.
     * Use this for reliable session state handling.
     */
    fun observeSessionStatus(): Flow<AuthSessionStatus>

    /**
     * Wait for the session to be loaded from storage.
     * Returns the user ID if authenticated, null otherwise.
     */
    suspend fun waitForSessionLoaded(): String?

    suspend fun logout()

    // ==================== STORAGE ====================

    suspend fun uploadImage(
        bucket: String,
        fileName: String,
        imageData: ByteArray
    ): String

    // ==================== USER ====================

    suspend fun getUser(userId: String): User?

    suspend fun updateUser(user: User): User

    suspend fun completeProfile(
        userId: String,
        name: String,
        city: String,
        interests: List<EventCategory>,
        avatarUrl: String?
    ): User

    // ==================== INVITE CODES ====================

    suspend fun validateInviteCode(code: String): InviteCode?

    suspend fun useInviteCode(code: String, userId: String): CommunityMembership

    suspend fun generateInviteCode(
        communityId: String,
        createdBy: String,
        maxUses: Int?,
        expiresInDays: Int?
    ): InviteCode

    suspend fun getUserInviteCodes(userId: String, communityId: String): List<InviteCode>

    // ==================== COMMUNITIES ====================

    suspend fun getCommunities(): List<CommunityGroup>

    suspend fun getUserCommunities(userId: String): List<Pair<CommunityGroup, com.run.peakflow.data.models.MembershipRole>>

    suspend fun getCommunitiesByCity(city: String): List<CommunityGroup>

    suspend fun getCommunityById(communityId: String): CommunityGroup?

    suspend fun getDiscoverCommunities(
        city: String,
        excludeUserCommunities: List<String>
    ): List<CommunityGroup>

    suspend fun searchCommunities(
        query: String,
        category: EventCategory?,
        city: String
    ): List<CommunityGroup>

    suspend fun createCommunity(
        title: String,
        description: String,
        category: EventCategory,
        city: String,
        rules: List<String>,
        createdBy: String,
        imageUrl: String?,
        coverUrl: String?
    ): CommunityGroup

    // ==================== MEMBERSHIPS ====================

    suspend fun getUserMemberships(userId: String): List<CommunityMembership>

    suspend fun getCommunityMemberships(communityId: String): List<CommunityMembership>

    /**
     * Batch-fetch community members with profile info (eliminates N+1 user lookups).
     * Returns triples of (membership, userName, userAvatarUrl).
     */
    suspend fun getCommunityMembersWithProfiles(
        communityId: String
    ): List<CommunityMemberWithProfile>

    suspend fun getMembershipRole(userId: String, communityId: String): CommunityMembership?

    suspend fun isUserMemberOf(userId: String, communityId: String): Boolean

    // ==================== JOIN REQUESTS ====================

    suspend fun requestToJoin(userId: String, communityId: String): JoinRequest

    suspend fun getPendingJoinRequests(communityId: String): List<JoinRequest>

    suspend fun getUserJoinRequests(userId: String): List<JoinRequest>

    suspend fun approveJoinRequest(
        requestId: String,
        reviewedBy: String
    ): CommunityMembership

    suspend fun rejectJoinRequest(
        requestId: String,
        reviewedBy: String
    ): JoinRequest

    suspend fun hasUserRequestedToJoin(userId: String, communityId: String): Boolean

    /**
     * Batch-fetch all community IDs where the user has a pending join request.
     */
    suspend fun getPendingJoinRequestCommunityIds(userId: String): Set<String>

    // ==================== POSTS ====================

    suspend fun getCommunityPosts(communityId: String): List<Post>

    suspend fun getFeedPosts(communityIds: List<String>): List<Post>

    suspend fun getPostById(postId: String): Post?

    suspend fun createPost(
        communityId: String,
        authorId: String,
        content: String,
        imageUrl: String?
    ): Post

    suspend fun deletePost(postId: String): Boolean

    suspend fun likePost(postId: String, userId: String): Boolean

    suspend fun unlikePost(postId: String, userId: String): Boolean

    suspend fun hasUserLikedPost(postId: String, userId: String): Boolean

    // ==================== COMMENTS ====================

    suspend fun getPostComments(postId: String): List<PostComment>

    suspend fun addComment(
        postId: String,
        userId: String,
        content: String
    ): PostComment

    suspend fun deleteComment(commentId: String): Boolean

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
        isFree: Boolean,
        price: Double?,
        imageUrl: String?
    ): Event

    suspend fun getEventsByGroupId(groupId: String): List<Event>

    /**
     * Get community events with the current user's RSVP status (eliminates N+1 RSVP checks).
     * Returns pairs of (Event, isRsvped).
     */
    suspend fun getCommunityEventsWithRsvp(communityId: String): List<Pair<Event, Boolean>>

    suspend fun getEventById(eventId: String): Event?

    suspend fun getNearbyEvents(
        city: String,
        category: EventCategory?,
        communityIds: List<String>
    ): List<Event>

    suspend fun getAllAccessibleEvents(communityIds: List<String>): List<Event>

    /**
     * Batch-fetch all events for the user's communities with RSVP status.
     * Eliminates the N+1 pattern of fetching events then checking RSVP for each.
     */
    suspend fun getUserEventsWithRsvp(category: EventCategory? = null): List<Pair<Event, Boolean>>

    /**
     * Fetch a single event with community info, RSVP status, and check-in status in one RPC call.
     * Eliminates 4 sequential calls in EventDetailComponent.
     */
    suspend fun getEventDetail(eventId: String): EventDetailResult?

    // ==================== RSVP ====================

    suspend fun rsvpToEvent(userId: String, eventId: String): Rsvp

    suspend fun cancelRsvp(userId: String, eventId: String): Boolean

    suspend fun getUserRsvps(userId: String): List<Rsvp>

    suspend fun getEventRsvps(eventId: String): List<Rsvp>

    suspend fun hasUserRsvped(userId: String, eventId: String): Boolean

    // ==================== ATTENDANCE ====================

    suspend fun checkInToEvent(userId: String, eventId: String): Attendance

    suspend fun hasUserCheckedIn(userId: String, eventId: String): Boolean

    suspend fun getEventAttendance(eventId: String): List<Attendance>

    suspend fun getUserAttendanceHistory(userId: String): List<Attendance>

    // ==================== REALTIME ====================

    fun observePosts(communityId: String): kotlinx.coroutines.flow.Flow<Post>

    fun observeComments(postId: String): kotlinx.coroutines.flow.Flow<PostComment>

    fun observeJoinRequests(communityId: String): kotlinx.coroutines.flow.Flow<JoinRequest>
}