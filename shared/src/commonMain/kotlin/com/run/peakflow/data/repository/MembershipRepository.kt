package com.run.peakflow.data.repository

import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.network.ApiService

class MembershipRepository(
    private val api: ApiService
) {
    // ==================== MEMBERSHIPS ====================

    suspend fun getUserMemberships(userId: String): List<CommunityMembership> {
        return api.getUserMemberships(userId)
    }

    suspend fun getCommunityMemberships(communityId: String): List<CommunityMembership> {
        return api.getCommunityMemberships(communityId)
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
        return api.approveJoinRequest(requestId, reviewedBy)
    }

    suspend fun rejectJoinRequest(requestId: String, reviewedBy: String): JoinRequest {
        return api.rejectJoinRequest(requestId, reviewedBy)
    }

    suspend fun hasUserRequestedToJoin(userId: String, communityId: String): Boolean {
        return api.hasUserRequestedToJoin(userId, communityId)
    }
}