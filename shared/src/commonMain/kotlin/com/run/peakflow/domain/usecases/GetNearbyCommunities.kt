package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.repository.CommunityRepository

class GetNearbyCommunities(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(city: String): List<CommunityGroup> {
        return repository.getCommunitiesByCity(city)
    }
}