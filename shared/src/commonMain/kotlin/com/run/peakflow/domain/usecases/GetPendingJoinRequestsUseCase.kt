package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.repository.MembershipRepository

class GetPendingJoinRequestsUseCase(
    private val membershipRepository: MembershipRepository
) {
    suspend operator fun invoke(communityId: String): List<JoinRequest> {
        return membershipRepository.getPendingJoinRequests(communityId)
    }
}