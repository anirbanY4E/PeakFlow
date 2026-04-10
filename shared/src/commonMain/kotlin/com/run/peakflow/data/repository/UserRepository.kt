package com.run.peakflow.data.repository

import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.ApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class UserRepository(
    private val api: ApiService
) {
    private val mutex = Mutex()

    // Cache of current user ID (backed by persistent Supabase session)
    private var currentUserId: String? = null

    // ==================== In-Memory Cache ====================

    private data class CachedUser(val user: User, val cachedAt: Long)

    /** Cache of user profiles by userId. TTL = 30 seconds. */
    private val userCache = mutableMapOf<String, CachedUser>()
    private val cacheTtlMs = 30_000L // 30 seconds

    private fun isCacheValid(cachedAt: Long): Boolean {
        return (Clock.System.now().toEpochMilliseconds() - cachedAt) < cacheTtlMs
    }

    // ==================== Public API ====================

    suspend fun setCurrentUserId(userId: String?) {
        mutex.withLock {
            currentUserId = userId
        }
    }

    suspend fun getCurrentUserId(): String? = mutex.withLock { currentUserId }

    suspend fun getUser(userId: String): User? {
        // Check cache first
        val cached = mutex.withLock { userCache[userId] }
        if (cached != null && isCacheValid(cached.cachedAt)) {
            return cached.user
        }
        // Fetch from network and cache
        val user = api.getUser(userId)
        if (user != null) {
            mutex.withLock {
                userCache[userId] = CachedUser(user, Clock.System.now().toEpochMilliseconds())
            }
        }
        return user
    }

    suspend fun getCurrentUser(): User? {
        val userId = getCurrentUserId() ?: return null
        return getUser(userId)
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
        val updated = api.updateUser(userToUpdate)
        // Invalidate cache so next read gets fresh data
        mutex.withLock {
            userCache[updated.id] = CachedUser(updated, Clock.System.now().toEpochMilliseconds())
        }
        return updated
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
        val user = api.completeProfile(userId, name, city, interests, avatarUrl)
        // Update cache with fresh profile
        mutex.withLock {
            userCache[user.id] = CachedUser(user, Clock.System.now().toEpochMilliseconds())
        }
        return user
    }

    /**
     * Checks if user is logged in by first checking in-memory cache,
     * then falling back to persistent session storage.
     * Call this on app startup to restore session from storage.
     */
    suspend fun isLoggedIn(): Boolean {
        // First check in-memory cache
        if (getCurrentUserId() != null) {
            return true
        }
        // Then check persistent session storage
        val sessionUserId = api.waitForSessionLoaded()
        if (sessionUserId != null) {
            setCurrentUserId(sessionUserId)
            return true
        }
        return false
    }

    suspend fun logout() {
        mutex.withLock {
            currentUserId = null
            userCache.clear()
        }
        api.logout()
    }
}