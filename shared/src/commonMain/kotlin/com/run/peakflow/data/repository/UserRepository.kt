package com.run.peakflow.data.repository

import com.run.peakflow.data.models.User
import com.run.peakflow.data.network.ApiService

class UserRepository(
    private val api: ApiService
) {
    private var cachedUser: User? = null

    suspend fun createUser(name: String, city: String): User {
        val user = api.createUser(name, city)
        cachedUser = user
        return user
    }

    suspend fun getUser(userId: String): User? {
        return api.getUser(userId)
    }

    fun getCachedUser(): User? = cachedUser

    fun setCachedUser(user: User) {
        cachedUser = user
    }

    fun isUserLoggedIn(): Boolean = cachedUser != null

    fun clearUser() {
        cachedUser = null
    }
}