import { Ratelimit } from "https://esm.sh/@upstash/ratelimit@1.0.0";
import { redis } from "./redis.ts";

// Define the limits
const ratelimits = {
  // 3 requests per hour
  'create-community': redis ? new Ratelimit({
    redis: redis,
    limiter: Ratelimit.slidingWindow(3, "1 h"),
    analytics: true,
  }) : null,
  
  // 10 requests per minute
  'generate-invite-code': redis ? new Ratelimit({
    redis: redis,
    limiter: Ratelimit.slidingWindow(10, "1 m"),
    analytics: true,
  }) : null,
  
  // 20 requests per minute
  'validate-invite-code': redis ? new Ratelimit({
    redis: redis,
    limiter: Ratelimit.slidingWindow(20, "1 m"),
    analytics: true,
  }) : null,
  
  // 5 requests per minute
  'use-invite-code': redis ? new Ratelimit({
    redis: redis,
    limiter: Ratelimit.slidingWindow(5, "1 m"),
    analytics: true,
  }) : null,
  
  // 30 requests per minute
  'approve-join-request': redis ? new Ratelimit({
    redis: redis,
    limiter: Ratelimit.slidingWindow(30, "1 m"),
    analytics: true,
  }) : null,
  
  // 30 requests per minute
  'reject-join-request': redis ? new Ratelimit({
    redis: redis,
    limiter: Ratelimit.slidingWindow(30, "1 m"),
    analytics: true,
  }) : null,
  
  // 60 requests per minute
  'send-push': redis ? new Ratelimit({
    redis: redis,
    limiter: Ratelimit.slidingWindow(60, "1 m"),
    analytics: true,
  }) : null,
};

type FunctionName = keyof typeof ratelimits;

/**
 * Checks the rate limit for a given function and identifier.
 * Returns null if allowed, or a Response object if blocked.
 */
export async function checkRateLimit(
  functionName: FunctionName, 
  identifier: string,
  corsHeaders: Record<string, string>
): Promise<Response | null> {
  const limiter = ratelimits[functionName];
  
  // If Redis credentials are not configured, degrade gracefully (allow)
  if (!limiter) {
    return null;
  }

  try {
    const { success, limit, remaining, reset } = await limiter.limit(identifier);
    
    if (!success) {
      return new Response(
        JSON.stringify({ error: "Too many requests. Please try again later." }),
        { 
          status: 429, 
          headers: { 
            ...corsHeaders,
            "Content-Type": "application/json",
            "X-RateLimit-Limit": limit.toString(),
            "X-RateLimit-Remaining": remaining.toString(),
            "X-RateLimit-Reset": reset.toString()
          } 
        }
      );
    }
  } catch (e) {
    console.error(`Rate limit evaluation failed for ${functionName}:`, e);
    // Fail-open: if Upstash is unreachable, let the request through
  }
  
  return null;
}
