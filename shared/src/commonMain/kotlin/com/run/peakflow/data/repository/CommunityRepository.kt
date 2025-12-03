package com.run.peakflow.data.repository

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.network.ApiService

class CommunityRepository(
    private val api: ApiService
) {
    suspend fun getCommunities(): List<CommunityGroup> {
        return api.getCommunities()
    }

    suspend fun getCommunitiesByCity(city: String): List<CommunityGroup> {
        return api.getCommunitiesByCity(city)
    }

    suspend fun getCommunityById(communityId: String): CommunityGroup? {
        return api.getCommunityById(communityId)
    }

    suspend fun getDiscoverCommunities(
        city: String,
        excludeUserCommunities: List<String>
    ): List<CommunityGroup> {
        return api.getDiscoverCommunities(city, excludeUserCommunities)
    }

    suspend fun getUserCommunities(
        userId: String,
        membershipRepository: MembershipRepository
    ): List<CommunityGroup> {
        val memberships = membershipRepository.getUserMemberships(userId)
        val communityIds = memberships.map { it.communityId }
        return getCommunities().filter { it.id in communityIds }
    }
}