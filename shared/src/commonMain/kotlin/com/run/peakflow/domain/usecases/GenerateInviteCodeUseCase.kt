package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.data.repository.InviteRepository
import com.run.peakflow.data.repository.UserRepository

class GenerateInviteCodeUseCase(
    private val inviteRepository: InviteRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        communityId: String,
        maxUses: Int? = null,
        expiresInDays: Int? = 7
    ): Result<InviteCode> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val invite = inviteRepository.generateInviteCode(
                communityId = communityId,
                createdBy = userId,
                maxUses = maxUses,
                expiresInDays = expiresInDays
            )
            Result.success(invite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}