import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { getOrSetCache, CacheKeys } from "../_shared/cache.ts"

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

    // Parse the payload which contains RPC name and arguments
    const { rpc, params } = await req.json()

    if (!rpc || !params) {
      return new Response(
        JSON.stringify({ error: 'Missing rpc or params in request body' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Verify the JWT cryptographically via Supabase Auth (not manual base64 decode)
    const { data: { user }, error: authError } = await supabaseClient.auth.getUser();
    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }
    const userId = user.id;

    let cacheKey = '';
    let ttlSeconds = 60; // default 60s
    let tags: string[] = [];
    let fetchFn = async () => {
      const { data, error } = await supabaseClient.rpc(rpc, params);
      if (error) throw error;
      return data;
    }

    // Determine cache key, TTL, and invalidation tags based on the requested RPC
    switch (rpc) {
      case 'get_user_feed':
        cacheKey = CacheKeys.userFeed(params.p_user_id, params.p_limit, params.p_offset);
        ttlSeconds = 60; // 1 min (frequently updated)
        tags = [`user:${params.p_user_id}:feed`];
        break;
      case 'get_community_posts':
        cacheKey = CacheKeys.communityPosts(params.p_community_id, params.p_limit, params.p_offset, userId);
        ttlSeconds = 60; // 1 min
        tags = [`community:${params.p_community_id}:posts`];
        break;
      case 'get_community_stats':
        cacheKey = CacheKeys.communityStats(params.p_community_id);
        ttlSeconds = 120; // 2 min
        tags = [`community:${params.p_community_id}:stats`];
        break;
      case 'search_communities':
        cacheKey = CacheKeys.searchCommunities(params.p_query, params.p_category, params.p_city);
        ttlSeconds = 120; // 2 min
        break;
      case 'get_community_events_with_rsvp':
        cacheKey = CacheKeys.communityEvents(params.p_community_id, userId);
        ttlSeconds = 180; // 3 min
        tags = [`community:${params.p_community_id}:events`, `user:${userId}:events`];
        break;
      case 'get_community_members_with_profiles':
        cacheKey = CacheKeys.communityMembers(params.p_community_id);
        ttlSeconds = 180; // 3 min
        tags = [`community:${params.p_community_id}:members`];
        break;
      case 'get_user_events_with_rsvp':
        cacheKey = CacheKeys.userEventsWithRsvp(params.p_user_id, params.p_category);
        ttlSeconds = 180; // 3 min
        tags = [`user:${params.p_user_id}:events`];
        break;
      case 'get_user_upcoming_events':
        cacheKey = CacheKeys.userUpcomingEvents(params.p_user_id);
        ttlSeconds = 180; // 3 min
        tags = [`user:${params.p_user_id}:events`];
        break;
      case 'get_user_communities':
        cacheKey = CacheKeys.userCommunities(params.p_user_id);
        ttlSeconds = 300; // 5 min
        tags = [`user:${params.p_user_id}:communities`];
        break;
      case 'get_discover_communities':
        cacheKey = CacheKeys.discoverCommunities(params.p_city, params.p_limit, userId);
        ttlSeconds = 300; // 5 min
        tags = ['discover'];
        break;
      case 'get_event_detail':
        cacheKey = CacheKeys.eventDetail(params.p_event_id, userId);
        ttlSeconds = 300; // 5 min
        tags = [`event:${params.p_event_id}`];
        break;
      default:
        // For unmapped RPCs, fallback to direct fetch without cache, or standard dynamic key
        cacheKey = `rpc:${rpc}:${JSON.stringify(params)}`;
        ttlSeconds = 30; // short default
        break;
    }

    const start = Date.now()
    
    // Attempt cache retrieval with stampede protection and tagging
    const data = await getOrSetCache(cacheKey, ttlSeconds, fetchFn, tags)
    
    const latency = Date.now() - start

    // Set a custom X-Cache header to let the client know how fast it was
    // (In reality this will always say HIT or MISS on a custom edge log but typically hard to determine without changing getOrSetCache return signature, so we just return the payload)
    return new Response(
      JSON.stringify(data),
      { 
        status: 200, 
        headers: { 
          ...corsHeaders, 
          'Content-Type': 'application/json',
          'X-Processing-Time': `${latency}ms`
        } 
      }
    )

  } catch (error) {
    console.error('Error in get-cached-data:', error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
