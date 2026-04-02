package com.run.peakflow.data.cache

import com.run.peakflow.data.models.User
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ProfileCache {
    private val cache = mutableMapOf<String, User>()
    private const val MAX_SIZE = 50
    private val mutex = Mutex()
    
    suspend fun get(userId: String): User? = mutex.withLock { cache[userId] }
    
    suspend fun put(userId: String, user: User) = mutex.withLock {
        if (cache.size >= MAX_SIZE && !cache.containsKey(userId)) {
            val firstKey = cache.keys.first()
            cache.remove(firstKey)
        }
        cache[userId] = user
    }
    
    suspend fun clear() = mutex.withLock { cache.clear() }
}
