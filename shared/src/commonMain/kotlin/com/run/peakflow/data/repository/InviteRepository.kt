package com.run.peakflow.data.repository

import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.data.network.ApiService

class InviteRepository(
    private val api: ApiService
) {
    suspend fun validateInviteCode(code: String): InviteCode? {
        return api.validateInviteCode(code)
    }

    suspend fun useInviteCode(code: String, userId: String): CommunityMembership {
        return api.useInviteCode(code, userId)
    }

    suspend fun generateInviteCode(
        communityId: String,
        createdBy: String,
        maxUses: Int? = null,
        expiresInDays: Int? = 7
    ): InviteCode {
        return api.generateInviteCode(communityId, createdBy, maxUses, expiresInDays)
    }

    suspend fun getUserInviteCodes(userId: String, communityId: String): List<InviteCode> {
        return api.getUserInviteCodes(userId, communityId)
    }
}