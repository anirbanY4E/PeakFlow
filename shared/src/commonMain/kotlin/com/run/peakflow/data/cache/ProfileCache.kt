package com.run.peakflow.data.cache

import com.run.peakflow.data.models.User
import kotlin.time.Clock
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ProfileCache {
    private val cache = mutableMapOf<String, Pair<User, Long>>()
    private val inFlightRequests = mutableMapOf<String, Deferred<User?>>()
    private const val MAX_SIZE = 100
    private const val TTL_MS = 15 * 60 * 1000L // 15 minutes
    private val mutex = Mutex()
    
    suspend fun get(userId: String): User? = mutex.withLock { 
        val cached = cache[userId]
        val now = Clock.System.now().toEpochMilliseconds()
        if (cached != null && (now - cached.second) <= TTL_MS) {
            cached.first 
        } else {
            cache.remove(userId)
            null
        }
    }
    
    suspend fun getOrFetch(userId: String, fetcher: suspend () -> User?): User? = coroutineScope {
        val now = Clock.System.now().toEpochMilliseconds()
        
        // 1. Check cache
        mutex.withLock {
            val cached = cache[userId]
            if (cached != null && (now - cached.second) <= TTL_MS) {
                return@coroutineScope cached.first
            }
        }

        // 2. Check for in-flight request
        val deferred = mutex.withLock {
            inFlightRequests[userId] ?: async {
                try {
                    val result = fetcher()
                    if (result != null) {
                        put(userId, result)
                    }
                    result
                } finally {
                    mutex.withLock { inFlightRequests.remove(userId) }
                }
            }.also { inFlightRequests[userId] = it }
        }

        deferred.await()
    }
    
    suspend fun put(userId: String, user: User) = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        if (cache.size >= MAX_SIZE && !cache.containsKey(userId)) {
            // Simple LRU: remove the oldest (first) entry
            val firstKey = cache.keys.firstOrNull()
            if (firstKey != null) cache.remove(firstKey)
        }
        cache[userId] = user to now
    }
    
    suspend fun clear() = mutex.withLock { 
        cache.clear() 
        inFlightRequests.values.forEach { it.cancel() }
        inFlightRequests.clear()
    }
}
