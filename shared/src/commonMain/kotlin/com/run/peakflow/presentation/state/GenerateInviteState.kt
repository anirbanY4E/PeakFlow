package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.InviteCode

data class GenerateInviteState(
    val community: CommunityGroup? = null,
    val generatedCode: InviteCode? = null,
    val existingCodes: List<InviteCode> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val maxUses: Int? = null,
    val expiresInDays: Int = 7,
    val isCopied: Boolean = false
)