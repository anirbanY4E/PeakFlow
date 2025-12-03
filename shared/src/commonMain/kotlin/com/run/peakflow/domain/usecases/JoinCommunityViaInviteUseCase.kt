package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.repository.AuthRepository
import com.run.peakflow.data.repository.InviteRepository

class JoinCommunityViaInviteUseCase(
    private val inviteRepository: InviteRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): Result<CommunityMembership> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val membership = inviteRepository.useInviteCode(code.trim().uppercase(), userId)
            authRepository.updateAuthState(hasCommunity = true)
            Result.success(membership)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}