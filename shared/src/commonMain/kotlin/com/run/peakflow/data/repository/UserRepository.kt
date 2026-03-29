package com.run.peakflow.data.repository

import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.ApiService
import kotlin.time.Clock
import kotlinx.datetime.Clock as KtClock

class UserRepository(
    private val api: ApiService
) {
    // Cache of current user ID (backed by persistent Supabase session)
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
        println("DEBUG UserRepo.updateUser: avatarBytes=${avatarBytes?.size ?: "null"}, existingAvatarUrl=${user.avatarUrl}")
        var updatedAvatarUrl = user.avatarUrl
        if (avatarBytes != null) {
            val time = Clock.System.now().toEpochMilliseconds()
            val fileName = "avatar_${user.id}_${time}.jpg"
            println("DEBUG UserRepo.updateUser: uploading $fileName (${avatarBytes.size} bytes)")
            updatedAvatarUrl = api.uploadImage("avatars", fileName, avatarBytes)
            println("DEBUG UserRepo.updateUser: upload result url=$updatedAvatarUrl")
        }
        val userToUpdate = user.copy(avatarUrl = updatedAvatarUrl)
        println("DEBUG UserRepo.updateUser: updating profile with avatarUrl=$updatedAvatarUrl")
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

    /**
     * Checks if user is logged in by first checking in-memory cache,
     * then falling back to persistent session storage.
     * Call this on app startup to restore session from storage.
     */
    suspend fun isLoggedIn(): Boolean {
        // First check in-memory cache
        if (currentUserId != null) {
            return true
        }
        // Then check persistent session storage
        val sessionUserId = api.waitForSessionLoaded()
        if (sessionUserId != null) {
            currentUserId = sessionUserId
            return true
        }
        return false
    }

    suspend fun logout() {
        currentUserId = null
        api.logout()
    }
}