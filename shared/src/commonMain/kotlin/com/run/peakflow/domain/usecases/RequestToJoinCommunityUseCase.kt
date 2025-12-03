package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.UserRepository

class RequestToJoinCommunityUseCase(
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(communityId: String): Result<JoinRequest> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Check if already member
            val isMember = membershipRepository.isUserMemberOf(userId, communityId)
            if (isMember) {
                return Result.failure(Exception("Already a member of this community"))
            }

            // Check if already requested
            val hasRequested = membershipRepository.hasUserRequestedToJoin(userId, communityId)
            if (hasRequested) {
                return Result.failure(Exception("Join request already pending"))
            }

            val request = membershipRepository.requestToJoin(userId, communityId)
            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}