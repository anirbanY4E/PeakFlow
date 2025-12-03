package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class HasUserRequestedToJoinUseCase(
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(communityId: String): Boolean {
        val userId = userRepository.getCurrentUserId() ?: return false
        return membershipRepository.hasUserRequestedToJoin(userId, communityId)
    }
}