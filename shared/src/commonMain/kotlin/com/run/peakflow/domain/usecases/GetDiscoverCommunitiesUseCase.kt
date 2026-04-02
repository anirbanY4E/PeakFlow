package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.repository.CommunityRepository
import com.run.peakflow.data.repository.UserRepository

class GetDiscoverCommunitiesUseCase(
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(city: String): List<CommunityGroup> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        return communityRepository.getDiscoverCommunities(userId, city)
    }
}