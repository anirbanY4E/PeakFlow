package com.run.peakflow.data.repository

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.network.ApiService
import kotlin.time.Clock

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

    suspend fun createCommunity(
        title: String,
        description: String,
        category: com.run.peakflow.data.models.EventCategory,
        city: String,
        rules: String,
        createdBy: String,
        imageBytes: ByteArray? = null,
        coverBytes: ByteArray? = null
    ): CommunityGroup {
        var imageUrl: String? = null
        var coverUrl: String? = null
        val time = Clock.System.now().toEpochMilliseconds()
        
        if (imageBytes != null) {
            val fileName = "community_${createdBy}_image_${time}.jpg"
            imageUrl = api.uploadImage("community-images", fileName, imageBytes)
        }
        if (coverBytes != null) {
            val fileName = "community_${createdBy}_cover_${time}.jpg"
            coverUrl = api.uploadImage("community-images", fileName, coverBytes)
        }
        val rulesList = rules.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        return api.createCommunity(title, description, category, city, rulesList, createdBy, imageUrl, coverUrl)
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