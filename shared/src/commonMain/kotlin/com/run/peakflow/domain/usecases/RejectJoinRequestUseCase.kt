package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class RejectJoinRequestUseCase(
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(requestId: String): Result<JoinRequest> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val request = membershipRepository.rejectJoinRequest(requestId, userId)
            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}