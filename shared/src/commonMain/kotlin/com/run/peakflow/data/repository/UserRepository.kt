package com.run.peakflow.data.repository

import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.ApiService
import kotlin.time.Clock
import kotlinx.datetime.Clock as KtClock

class UserRepository(
    private val api: ApiService
) {
    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String?) {
        currentUserId = userId
    }

    fun getCurrentUserId(): String? = currentUserId

    suspend fun getUser(userId: String): User? {
        return api.getUser(userId)
    }

    suspend fun getCurrentUser(): User? {
        val userId = currentUserId ?: return null
        return api.getUser(userId)
    }

    suspend fun updateUser(
        user: User,
        avatarBytes: ByteArray? = null
    ): User {
        var updatedAvatarUrl = user.avatarUrl
        if (avatarBytes != null) {
            val time = Clock.System.now().toEpochMilliseconds()
            val fileName = "avatar_${user.id}_${time}.jpg"
            updatedAvatarUrl = api.uploadImage("avatars", fileName, avatarBytes)
        }
        val userToUpdate = user.copy(avatarUrl = updatedAvatarUrl)
        return api.updateUser(userToUpdate)
    }

    suspend fun completeProfile(
        userId: String,
        name: String,
        city: String,
        interests: List<EventCategory>,
        avatarBytes: ByteArray? = null
    ): User {
        var avatarUrl: String? = null
        if (avatarBytes != null) {
            val time = Clock.System.now().toEpochMilliseconds()
            val fileName = "avatar_${userId}_${time}.jpg"
            avatarUrl = api.uploadImage("avatars", fileName, avatarBytes)
        }
        return api.completeProfile(userId, name, city, interests, avatarUrl)
    }

    suspend fun isLoggedIn(): Boolean {
        if (currentUserId == null) {
            currentUserId = api.getSessionUserId()
        }
        return currentUserId != null
    }

    suspend fun logout() {
        currentUserId = null
        api.logout()
    }
}