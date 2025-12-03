package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.data.repository.InviteRepository

class ValidateInviteCodeUseCase(
    private val inviteRepository: InviteRepository
) {
    suspend operator fun invoke(code: String): Result<InviteCode> {
        return try {
            if (code.isBlank()) {
                return Result.failure(Exception("Invite code is required"))
            }
            val invite = inviteRepository.validateInviteCode(code.trim().uppercase())
            if (invite != null) {
                Result.success(invite)
            } else {
                Result.failure(Exception("Invalid or expired invite code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}