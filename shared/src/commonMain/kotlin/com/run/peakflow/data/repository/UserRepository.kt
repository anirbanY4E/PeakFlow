package com.run.peakflow.data.repository

import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.ApiService

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

    suspend fun updateUser(user: User): User {
        return api.updateUser(user)
    }

    suspend fun completeProfile(
        userId: String,
        name: String,
        city: String,
        interests: List<EventCategory>,
        avatarUrl: String?
    ): User {
        return api.completeProfile(userId, name, city, interests, avatarUrl)
    }

    fun isLoggedIn(): Boolean = currentUserId != null

    fun logout() {
        currentUserId = null
    }
}