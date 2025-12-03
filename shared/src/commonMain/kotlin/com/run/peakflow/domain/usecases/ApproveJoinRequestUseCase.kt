package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class ApproveJoinRequestUseCase(
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(requestId: String): Result<CommunityMembership> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val membership = membershipRepository.approveJoinRequest(requestId, userId)
            Result.success(membership)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}