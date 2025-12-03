package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.repository.CommunityRepository
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class GetUserCommunitiesUseCase(
    private val communityRepository: CommunityRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<CommunityGroup> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        return communityRepository.getUserCommunities(userId, membershipRepository)
    }
}