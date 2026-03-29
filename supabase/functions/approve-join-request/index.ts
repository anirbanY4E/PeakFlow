// Supabase Edge Function: Approve Join Request
// Admin-only: Approves a pending join request and creates membership

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

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
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: req.headers.get('Authorization')! },
        },
      }
    )

    // Admin client for operations that bypass RLS
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

    const { requestId } = await req.json()

    if (!requestId) {
      return new Response(
        JSON.stringify({ error: 'Request ID is required' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 1. Get the join request
    const { data: joinRequest, error: requestError } = await supabaseClient
      .from('join_requests')
      .select('*')
      .eq('id', requestId)
      .eq('status', 'PENDING')
      .single()

    if (requestError || !joinRequest) {
      return new Response(
        JSON.stringify({ error: 'Join request not found or already processed' }),
        { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 2. Verify the user is an admin of the community
    const { data: adminMembership, error: adminError } = await supabaseClient
      .from('memberships')
      .select('*')
      .eq('user_id', user.id)
      .eq('community_id', joinRequest.community_id)
      .eq('role', 'ADMIN')
      .single()

    if (adminError || !adminMembership) {
      return new Response(
        JSON.stringify({ error: 'Only community admins can approve join requests' }),
        { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 3. Check if user is already a member (edge case)
    const { data: existingMembership } = await supabaseAdmin
      .from('memberships')
      .select('id')
      .eq('user_id', joinRequest.user_id)
      .eq('community_id', joinRequest.community_id)
      .maybeSingle()

    if (existingMembership) {
      // Update the request to rejected since they're already a member
      await supabaseAdmin
        .from('join_requests')
        .update({
          status: 'REJECTED',
          reviewed_at: new Date().toISOString(),
          reviewed_by: user.id,
        })
        .eq('id', requestId)

      return new Response(
        JSON.stringify({ error: 'User is already a member of this community' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 4. Create membership using admin client
    const { data: membership, error: membershipError } = await supabaseAdmin
      .from('memberships')
      .insert({
        user_id: joinRequest.user_id,
        community_id: joinRequest.community_id,
        role: 'MEMBER',
        invited_by: user.id,
      })
      .select()
      .single()

    if (membershipError) {
      console.error('Membership error:', membershipError)
      return new Response(
        JSON.stringify({ error: 'Failed to create membership' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 5. Update the join request status
    await supabaseAdmin
      .from('join_requests')
      .update({
        status: 'APPROVED',
        reviewed_at: new Date().toISOString(),
        reviewed_by: user.id,
      })
      .eq('id', requestId)

    // 6. Get user profile to return in response
    const { data: userProfile } = await supabaseClient
      .from('profiles')
      .select('*')
      .eq('id', joinRequest.user_id)
      .single()

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
        },
        user: userProfile ? {
          id: userProfile.id,
          name: userProfile.name,
          email: userProfile.email,
          avatarUrl: userProfile.avatar_url,
        } : null,
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