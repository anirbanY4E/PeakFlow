// Supabase Edge Function: Generate Invite Code
// Admin-only: Generates a new invite code for a community

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { checkRateLimit } from "../_shared/rate-limiter.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

function generateInviteCode(prefix: string): string {
  const suffix = Math.random().toString(36).substring(2, 6).toUpperCase()
  return `${prefix}-${suffix}`
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

    // Get the authenticated user
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser()

    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Rate limiting check
    const rateLimitResponse = await checkRateLimit('generate-invite-code', user.id, corsHeaders);
    if (rateLimitResponse) return rateLimitResponse;

    const { communityId, maxUses, expiresInDays } = await req.json()

    if (!communityId) {
      return new Response(
        JSON.stringify({ error: 'Community ID is required' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 1. Verify the user is an admin of the community
    const { data: membership, error: membershipError } = await supabaseClient
      .from('memberships')
      .select('*')
      .eq('user_id', user.id)
      .eq('community_id', communityId)
      .eq('role', 'ADMIN')
      .single()

    if (membershipError || !membership) {
      return new Response(
        JSON.stringify({ error: 'Only community admins can generate invite codes' }),
        { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 2. Get community for code prefix
    const { data: community, error: communityError } = await supabaseClient
      .from('communities')
      .select('title')
      .eq('id', communityId)
      .single()

    if (communityError || !community) {
      return new Response(
        JSON.stringify({ error: 'Community not found' }),
        { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // 3. Generate code prefix from community title
    const words = community.title.split(' ')
    const prefix = words.length >= 2
      ? words.slice(0, 2).map(w => w.substring(0, 4).toUpperCase()).join('-')
      : words[0].substring(0, 8).toUpperCase()

    const code = generateInviteCode(prefix)

    // 4. Calculate expiration date if provided
    const expiresAt = expiresInDays
      ? new Date(Date.now() + expiresInDays * 24 * 60 * 60 * 1000).toISOString()
      : null

    // 5. Create the invite code
    const { data: inviteCode, error: createError } = await supabaseClient
      .from('invite_codes')
      .insert({
        code,
        community_id: communityId,
        created_by: user.id,
        max_uses: maxUses || null,
        expires_at: expiresAt,
      })
      .select()
      .single()

    if (createError) {
      console.error('Create error:', createError)
      return new Response(
        JSON.stringify({ error: 'Failed to generate invite code' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    return new Response(
      JSON.stringify({
        success: true,
        inviteCode: {
          id: inviteCode.id,
          code: inviteCode.code,
          communityId: inviteCode.community_id,
          createdBy: inviteCode.created_by,
          maxUses: inviteCode.max_uses,
          currentUses: inviteCode.current_uses,
          expiresAt: inviteCode.expires_at,
          isActive: inviteCode.is_active,
          createdAt: inviteCode.created_at,
        }
      }),
      { status: 201, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})