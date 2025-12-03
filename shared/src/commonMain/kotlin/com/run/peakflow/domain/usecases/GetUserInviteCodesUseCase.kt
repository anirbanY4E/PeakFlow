package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.data.repository.InviteRepository
import com.run.peakflow.data.repository.UserRepository

class GetUserInviteCodesUseCase(
    private val inviteRepository: InviteRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(communityId: String): List<InviteCode> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        return inviteRepository.getUserInviteCodes(userId, communityId)
    }
}