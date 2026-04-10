// Supabase Edge Function: Invalidate Cache
import { invalidateCacheKeys, invalidateCachePattern, invalidateCacheTags, CacheKeys } from "../_shared/cache.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: req.headers.get('Authorization')! },
        },
      }
    )

    // Ensure only authenticated users can use this specific route
    // if verify_jwt is false on the project, we still verify via supabase API calls 
    const { data: { user }, error: authError } = await supabaseClient.auth.getUser();
    if (authError || !user) {
       return new Response(JSON.stringify({error: "Unauthorized"}), { status: 401, headers: corsHeaders});
    }

    const { action, id, communityId, userId } = await req.json()

    // Process O(1) tag-based invalidations instead of O(N) pattern SCANs
    switch (action) {
      case 'new_post':
      case 'delete_post':
      case 'like_post':
      case 'unlike_post':
        if (communityId) {
           await invalidateCacheTags([`community:${communityId}:posts`]);
           await invalidateCachePattern(`user:*:feed:*`); // Feeds don't have communityId directly without scanning all users. But actually we tagged feeds globally before? Wait, I tagged user feeds as user:UID:feed, but how to invalidate ALL feeds? We still need to invalidateCachePattern(`user:*:feed:*`) because we don't know which users have this community in their feed.
           await invalidateCacheKeys([CacheKeys.communityStats(communityId)]);
        }
        break;
      case 'rsvp_event':
      case 'unrsvp_event':
        if (communityId && userId && id) {
           await invalidateCacheTags([
               `event:${id}`, 
               `community:${communityId}:events`, 
               `user:${userId}:events`
           ]);
           await invalidateCacheKeys([
               CacheKeys.userUpcomingEvents(userId)
           ]);
        }
        break;
      case 'update_profile':
        if (userId) {
           await invalidateCachePattern(`community:*:members`); // We still use pattern here unless tracked globally, or just wildcard.
        }
        break;
      case 'leave_community':
        if (communityId && userId) {
            await invalidateCacheTags([`user:${userId}:communities`, `discover`]);
            await invalidateCacheKeys([
               CacheKeys.communityMembers(communityId),
               CacheKeys.communityStats(communityId)
            ]);
        }
        break;
      default:
        return new Response(JSON.stringify({ error: `Unknown invalidation action: ${action}` }), { status: 400, headers: corsHeaders });
    }

    return new Response(
      JSON.stringify({ success: true, action: action }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Error in invalidate-cache:', error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
