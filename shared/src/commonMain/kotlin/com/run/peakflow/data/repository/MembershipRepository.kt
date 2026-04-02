package com.run.peakflow.data.repository

import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.network.ApiService
import com.run.peakflow.data.network.CommunityMemberWithProfile
import kotlin.time.Clock

class MembershipRepository(
    private val api: ApiService
) {
    // ==================== In-Memory Cache ====================

    private data class CachedMemberships(
        val memberships: List<CommunityMembership>,
        val cachedAt: Long
    )

    /** Cache of user memberships by userId. TTL = 30 seconds. */
    private val membershipCache = mutableMapOf<String, CachedMemberships>()
    private val cacheTtlMs = 30_000L // 30 seconds

    private fun isCacheValid(cachedAt: Long): Boolean {
        return (Clock.System.now().toEpochMilliseconds() - cachedAt) < cacheTtlMs
    }

    /** Invalidate membership cache for a specific user (or all users). */
    fun invalidateMembershipCache(userId: String? = null) {
        if (userId != null) {
            membershipCache.remove(userId)
        } else {
            membershipCache.clear()
        }
    }

    // ==================== MEMBERSHIPS ====================

    suspend fun getUserMemberships(userId: String): List<CommunityMembership> {
        // Check cache first
        val cached = membershipCache[userId]
        if (cached != null && isCacheValid(cached.cachedAt)) {
            return cached.memberships
        }
        // Fetch from network and cache
        val memberships = api.getUserMemberships(userId)
        membershipCache[userId] = CachedMemberships(memberships, Clock.System.now().toEpochMilliseconds())
        return memberships
    }

    suspend fun getCommunityMemberships(communityId: String): List<CommunityMembership> {
        return api.getCommunityMemberships(communityId)
    }

    suspend fun getCommunityMembersWithProfiles(communityId: String): List<CommunityMemberWithProfile> {
        return api.getCommunityMembersWithProfiles(communityId)
    }

    suspend fun getMembershipRole(userId: String, communityId: String): CommunityMembership? {
        return api.getMembershipRole(userId, communityId)
    }

    suspend fun isUserMemberOf(userId: String, communityId: String): Boolean {
        return api.isUserMemberOf(userId, communityId)
    }

    // ==================== JOIN REQUESTS ====================

    suspend fun requestToJoin(userId: String, communityId: String): JoinRequest {
        return api.requestToJoin(userId, communityId)
    }

    suspend fun getPendingJoinRequests(communityId: String): List<JoinRequest> {
        return api.getPendingJoinRequests(communityId)
    }

    suspend fun getUserJoinRequests(userId: String): List<JoinRequest> {
        return api.getUserJoinRequests(userId)
    }

    suspend fun approveJoinRequest(requestId: String, reviewedBy: String): CommunityMembership {
        val result = api.approveJoinRequest(requestId, reviewedBy)
        // Invalidate cache since memberships changed
        membershipCache.clear()
        return result
    }

    suspend fun rejectJoinRequest(requestId: String, reviewedBy: String): JoinRequest {
        return api.rejectJoinRequest(requestId, reviewedBy)
    }

    suspend fun hasUserRequestedToJoin(userId: String, communityId: String): Boolean {
        return api.hasUserRequestedToJoin(userId, communityId)
    }

    suspend fun getPendingJoinRequestCommunityIds(userId: String): Set<String> {
        return api.getPendingJoinRequestCommunityIds(userId)
    }

    // ==================== REALTIME ====================

    fun observeNewJoinRequests(communityId: String): kotlinx.coroutines.flow.Flow<JoinRequest> {
        return api.observeJoinRequests(communityId)
    }
}