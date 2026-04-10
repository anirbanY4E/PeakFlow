package com.run.peakflow.data.cache

import com.run.peakflow.data.models.CommunityGroup
import kotlin.time.Clock
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DataCache {
    private val communityCache = mutableMapOf<String, Pair<CommunityGroup, Long>>()
    private val inFlightRequests = mutableMapOf<String, Deferred<CommunityGroup?>>()
    private const val TTL_MS = 5 * 60 * 1000L  // 5 minutes
    private val mutex = Mutex()
    
    suspend fun getOrFetchCommunity(id: String, fetcher: suspend () -> CommunityGroup?): CommunityGroup? = coroutineScope {
        val now = Clock.System.now().toEpochMilliseconds()
        
        // 1. Check cache
        mutex.withLock {
            val cached = communityCache[id]
            if (cached != null && (now - cached.second) <= TTL_MS) {
                return@coroutineScope cached.first
            }
        }

        // 2. Check for in-flight request (Thundering Herd prevention)
        val deferred = mutex.withLock {
            inFlightRequests[id] ?: async {
                try {
                    val result = fetcher()
                    if (result != null) {
                        mutex.withLock {
                            communityCache[id] = result to Clock.System.now().toEpochMilliseconds()
                        }
                    }
                    result
                } finally {
                    mutex.withLock { inFlightRequests.remove(id) }
                }
            }.also { inFlightRequests[id] = it }
        }

        deferred.await()
    }
    
    suspend fun invalidateCommunity(id: String) = mutex.withLock { 
        communityCache.remove(id)
        inFlightRequests.remove(id)?.cancel()
    }
    
    suspend fun clear() = mutex.withLock { 
        communityCache.clear() 
        inFlightRequests.values.forEach { it.cancel() }
        inFlightRequests.clear()
    }
}
