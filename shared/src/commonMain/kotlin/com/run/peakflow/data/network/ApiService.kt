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

    suspend fun verifyOtp(
        userId: String,
        otp: String
    ): Boolean

    suspend fun resendOtp(userId: String): Boolean

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

    suspend fun getCommunitiesByCity(city: String): List<CommunityGroup>

    suspend fun getCommunityById(communityId: String): CommunityGroup?

    suspend fun getDiscoverCommunities(
        city: String,
        excludeUserCommunities: List<String>
    ): List<CommunityGroup>

    // ==================== MEMBERSHIPS ====================

    suspend fun getUserMemberships(userId: String): List<CommunityMembership>

    suspend fun getCommunityMemberships(communityId: String): List<CommunityMembership>

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
        price: Double?
    ): Event

    suspend fun getEventsByGroupId(groupId: String): List<Event>

    suspend fun getEventById(eventId: String): Event?

    suspend fun getNearbyEvents(
        city: String,
        category: EventCategory?,
        communityIds: List<String>
    ): List<Event>

    suspend fun getAllAccessibleEvents(communityIds: List<String>): List<Event>

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
}