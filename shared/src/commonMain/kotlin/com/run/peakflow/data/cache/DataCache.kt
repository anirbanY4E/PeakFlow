package com.run.peakflow.data.cache

import com.run.peakflow.data.models.CommunityGroup
import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DataCache {
    private val communityCache = mutableMapOf<String, Pair<CommunityGroup, Long>>()
    private const val TTL_MS = 5 * 60 * 1000L  // 5 minutes
    private val mutex = Mutex()
    
    suspend fun getOrFetchCommunity(id: String, fetcher: suspend () -> CommunityGroup?): CommunityGroup? {
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            val cached = communityCache[id]
            if (cached != null && (now - cached.second) <= TTL_MS) {
                return cached.first
            }
        }
        
        val result = fetcher()
        
        if (result != null) {
            mutex.withLock {
                communityCache[id] = result to now
            }
        }
        return result
    }
    
    suspend fun invalidateCommunity(id: String) = mutex.withLock { communityCache.remove(id) }
    suspend fun clear() = mutex.withLock { communityCache.clear() }
}
