import { redis } from "./redis.ts";

/**
 * Gets a value from cache or executes fetchFn and stores the result
 * Implements cache stampede protection via distributed Redis locks
 */
export async function getOrSetCache<T>(
  key: string,
  ttlSeconds: number,
  fetchFn: () => Promise<T>,
  tags: string[] = []
): Promise<T> {
  if (!redis) {
    return await fetchFn();
  }

  // 1. First fast check
  try {
    const cachedItem = await redis.get<T>(key);
    if (cachedItem) {
      return cachedItem;
    }
  } catch (e) {
    console.error(`Error reading cache for key ${key}:`, e);
  }

  // 2. Cache miss. Acquire distributed lock for Cache Stampede protection
  const lockKey = `lock:${key}`;
  let lockAcquired = false;
  try {
    // Set lock with EX (expiry 10s) and NX (only if not exists)
    // using raw upstash command or set config
    lockAcquired = await redis.set(lockKey, "1", { ex: 10, nx: true }) === "OK";
  } catch (e) {
    console.error(`Error acquiring lock ${lockKey}:`, e);
  }

  if (!lockAcquired) {
    // Another request is currently fetching. Wait briefly then check cache again.
    await new Promise(r => setTimeout(r, 200));
    try {
      const retriedItem = await redis.get<T>(key);
      if (retriedItem) return retriedItem;
    } catch (e) {
      // ignore
    }
    // Fallback to direct fetch if still missing
    return await fetchFn();
  }

  // 3. We hold the lock. Fetch fresh data.
  const data = await fetchFn();

  try {
    if (data !== null && data !== undefined) {
      // Atomic pipeline would be better, but serial awaits are OK here
      await redis.set(key, data, { ex: ttlSeconds });
      
      // Store in tag sets if tags provided
      if (tags.length > 0) {
        for (const tag of tags) {
          // Add key to tag set and ensure the set expires eventually
          // (Since we don't know the exact max TTL, we set tag set TTL generously)
          await redis.sadd(`tag:${tag}`, key);
          await redis.expire(`tag:${tag}`, 86400); // 24 hours
        }
      }
    }
  } catch (e) {
    console.error(`Error successfully setting cache for key ${key}:`, e);
  } finally {
    // Release the lock
    try {
      await redis.del(lockKey);
    } catch (e) {}
  }

  return data;
}

/**
 * Invalidate a specific key
 */
export async function invalidateCacheKey(key: string): Promise<void> {
  if (!redis) return;
  try {
    await redis.del(key);
  } catch (e) {
    console.error(`Error deleting cache key ${key}:`, e);
  }
}

/**
 * Invalidate multiple keys
 */
export async function invalidateCacheKeys(keys: string[]): Promise<void> {
  if (!redis || keys.length === 0) return;
  try {
    await redis.del(...keys);
  } catch (e) {
    console.error(`Error deleting multiple cache keys:`, e);
  }
}

/**
 * Invalidate keys matching a pattern (O(N) - use tags instead when possible)
 */
export async function invalidateCachePattern(pattern: string): Promise<void> {
  if (!redis) return;
  try {
    let cursor = 0;
    do {
      const [nextCursor, keys] = await redis.scan(cursor, { match: pattern, count: 100 });
      cursor = nextCursor;
      if (keys.length > 0) {
        await redis.del(...keys);
      }
    } while (cursor !== 0);
  } catch (e) {
    console.error(`Error invalidating keys matching pattern ${pattern}:`, e);
  }
}

/**
 * Invalidate cache by tags (O(1) lookup)
 */
export async function invalidateCacheTags(tags: string[]): Promise<void> {
  if (!redis || tags.length === 0) return;
  try {
    const keysToDelete = new Set<string>();
    
    for (const tag of tags) {
      const tagKey = `tag:${tag}`;
      const keys = await redis.smembers(tagKey);
      keys.forEach((k: string) => keysToDelete.add(k));
      keysToDelete.add(tagKey); // also delete the tag set itself
    }

    if (keysToDelete.size > 0) {
      await redis.del(...Array.from(keysToDelete));
    }
  } catch (e) {
    console.error(`Error invalidating tags ${tags}:`, e);
  }
}

/**
 * Common cache key generators
 */
export const CacheKeys = {
  community: (id: string) => `community:${id}`,
  communityStats: (id: string) => `community:${id}:stats`,
  communityMembers: (id: string) => `community:${id}:members`,
  communityEvents: (communityId: string, userId: string) => `community:${communityId}:events:user:${userId}`,
  communityPosts: (communityId: string, limit: number, offset: number, userId: string) => `community:${communityId}:posts:${limit}:${offset}:user:${userId}`,
  userCommunities: (userId: string) => `user:${userId}:communities`,
  userFeed: (userId: string, limit: number, offset: number) => `user:${userId}:feed:${limit}:${offset}`,
  userUpcomingEvents: (userId: string) => `user:${userId}:upcoming-events`,
  userEventsWithRsvp: (userId: string, category: string | null) => `user:${userId}:events:${category || 'all'}`,
  discoverCommunities: (city: string, limit: number, userId: string) => `discover:${city}:${limit}:user:${userId}`,
  searchCommunities: (query: string, category: string | null, city: string) => `search:${query}:${category || 'all'}:${city}`,
  eventDetail: (id: string, userId?: string) => userId ? `event:${id}:user:${userId}` : `event:${id}`,
};
