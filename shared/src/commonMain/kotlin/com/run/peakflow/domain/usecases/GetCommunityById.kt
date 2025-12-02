package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.repository.CommunityRepository

class GetCommunityById(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(communityId: String): CommunityGroup? {
        return repository.getCommunityById(communityId)
    }
}