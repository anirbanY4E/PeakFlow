package com.run.peakflow.data.repository

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.network.ApiService

class CommunityRepository(
    private val api: ApiService
) {
    suspend fun getAllCommunities(): List<CommunityGroup> {
        return api.getCommunities()
    }

    suspend fun getCommunitiesByCity(city: String): List<CommunityGroup> {
        return api.getCommunitiesByCity(city)
    }

    suspend fun getCommunityById(communityId: String): CommunityGroup? {
        return api.getCommunityById(communityId)
    }
}