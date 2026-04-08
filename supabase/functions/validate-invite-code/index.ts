// Supabase Edge Function: Validate Invite Code
// Validates an invite code without using it

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { checkRateLimit } from "../_shared/rate-limiter.ts"

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

    // Rate limit by IP address for anonymous endpoints
    const clientIp = req.headers.get("x-forwarded-for") || "unknown-ip"
    const rateLimitResponse = await checkRateLimit('validate-invite-code', clientIp, corsHeaders);
    if (rateLimitResponse) return rateLimitResponse;

    const { code } = await req.json()

    if (!code) {
      return new Response(
        JSON.stringify({ error: 'Invite code is required' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Find the invite code (case-insensitive)
    const { data: inviteCode, error: inviteError } = await supabaseClient
      .from('invite_codes')
      .select('*, communities(id, title, category, city)')
      .ilike('code', code.toUpperCase())
      .eq('is_active', true)
      .single()

    if (inviteError || !inviteCode) {
      return new Response(
        JSON.stringify({ error: 'Invalid invite code' }),
        { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Check if expired
    if (inviteCode.expires_at && new Date(inviteCode.expires_at) < new Date()) {
      return new Response(
        JSON.stringify({ error: 'Invite code has expired' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Check if max uses reached
    if (inviteCode.max_uses !== null && inviteCode.current_uses >= inviteCode.max_uses) {
      return new Response(
        JSON.stringify({ error: 'Invite code has reached maximum uses' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    return new Response(
      JSON.stringify({
        valid: true,
        inviteCode: {
          id: inviteCode.id,
          code: inviteCode.code,
          communityId: inviteCode.community_id,
          community: inviteCode.communities,
          maxUses: inviteCode.max_uses,
          currentUses: inviteCode.current_uses,
          expiresAt: inviteCode.expires_at,
        }
      }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})