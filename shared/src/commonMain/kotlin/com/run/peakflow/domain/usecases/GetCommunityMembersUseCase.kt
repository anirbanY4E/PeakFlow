package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.repository.MembershipRepository

class GetCommunityMembersUseCase(
    private val membershipRepository: MembershipRepository
) {
    suspend operator fun invoke(communityId: String): List<CommunityMembership> {
        return membershipRepository.getCommunityMemberships(communityId)
    }
}