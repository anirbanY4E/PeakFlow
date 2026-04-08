// Supabase Edge Function: Use Invite Code
// Validates and uses an invite code to add user to a community

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { checkRateLimit } from "../_shared/rate-limiter.ts"
import { invalidateCachePattern, CacheKeys, invalidateCacheKeys } from "../_shared/cache.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Create client with user's auth context
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: req.headers.get('Authorization')! },
        },
      }
    )

    // Create admin client for operations that bypass RLS
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Get the authenticated user
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser()

    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Rate limiting check
    const rateLimitResponse = await checkRateLimit('use-invite-code', user.id, corsHeaders);
    if (rateLimitResponse) return rateLimitResponse;

    const { code } = await req.json()

    if (!code) {
      return new Response(
        JSON.stringify({ error: 'Invite code is required' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 1. Validate the invite code
    const { data: inviteCode, error: inviteError } = await supabaseClient
      .from('invite_codes')
      .select('*')
      .ilike('code', code.toUpperCase())
      .eq('is_active', true)
      .single()

    if (inviteError || !inviteCode) {
      return new Response(
        JSON.stringify({ error: 'Invalid invite code' }),
        { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 2. Check if expired
    if (inviteCode.expires_at && new Date(inviteCode.expires_at) < new Date()) {
      return new Response(
        JSON.stringify({ error: 'Invite code has expired' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 3. Check if max uses reached
    if (inviteCode.max_uses !== null && inviteCode.current_uses >= inviteCode.max_uses) {
      return new Response(
        JSON.stringify({ error: 'Invite code has reached maximum uses' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 4. Check if user is already a member
    const { data: existingMembership } = await supabaseClient
      .from('memberships')
      .select('*')
      .eq('user_id', user.id)
      .eq('community_id', inviteCode.community_id)
      .single()

    if (existingMembership) {
      return new Response(
        JSON.stringify({ error: 'You are already a member of this community' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 5. Check if user has a pending join request
    const { data: pendingRequest } = await supabaseClient
      .from('join_requests')
      .select('*')
      .eq('user_id', user.id)
      .eq('community_id', inviteCode.community_id)
      .eq('status', 'PENDING')
      .single()

    if (pendingRequest) {
      // Delete the pending request since they're joining via invite
      await supabaseAdmin
        .from('join_requests')
        .delete()
        .eq('id', pendingRequest.id)
    }

    // 6. Create membership using admin client (bypasses RLS)
    const { data: membership, error: membershipError } = await supabaseAdmin
      .from('memberships')
      .insert({
        user_id: user.id,
        community_id: inviteCode.community_id,
        role: 'MEMBER',
        invited_by: inviteCode.created_by,
      })
      .select('*, communities(*)')
      .single()

    if (membershipError) {
      console.error('Membership error:', membershipError)
      return new Response(
        JSON.stringify({ error: 'Failed to create membership' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 7. Increment invite code usage
    await supabaseAdmin
      .from('invite_codes')
      .update({ current_uses: inviteCode.current_uses + 1 })
      .eq('id', inviteCode.id)

    // 8. Invalidate caches
    await invalidateCacheKeys([
      CacheKeys.communityMembers(inviteCode.community_id),
      CacheKeys.communityStats(inviteCode.community_id),
      CacheKeys.userCommunities(user.id)
    ]);
    await invalidateCachePattern(`user:${user.id}:feed:*`);
    await invalidateCachePattern(`discover:*`);

    return new Response(
      JSON.stringify({
        success: true,
        membership: {
          id: membership.id,
          userId: membership.user_id,
          communityId: membership.community_id,
          role: membership.role,
          joinedAt: membership.joined_at,
          invitedBy: membership.invited_by,
          community: membership.communities,
        }
      }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})