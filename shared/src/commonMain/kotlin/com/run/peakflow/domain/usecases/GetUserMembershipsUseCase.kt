package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class GetUserMembershipsUseCase(
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<CommunityMembership> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        return membershipRepository.getUserMemberships(userId)
    }
}