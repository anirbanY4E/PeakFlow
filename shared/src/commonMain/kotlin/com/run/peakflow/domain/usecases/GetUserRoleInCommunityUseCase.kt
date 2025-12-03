package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class GetUserRoleInCommunityUseCase(
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(communityId: String): MembershipRole? {
        val userId = userRepository.getCurrentUserId() ?: return null
        return membershipRepository.getMembershipRole(userId, communityId)?.role
    }
}