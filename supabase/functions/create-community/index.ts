// Supabase Edge Function: Create Community
// Creates a new community with the creator as admin

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

    // Get the authenticated user
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser()

    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const { title, description, category, city, imageUrl, coverUrl, rules } = await req.json()

    if (!title || !description || !category || !city) {
      return new Response(
        JSON.stringify({ error: 'Title, description, category, and city are required' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Create the community (the trigger will automatically create admin membership)
    const { data: community, error: createError } = await supabaseClient
      .from('communities')
      .insert({
        title,
        description,
        category,
        city,
        created_by: user.id,
        image_url: imageUrl || null,
        cover_url: coverUrl || null,
        rules: rules || [],
      })
      .select()
      .single()

    if (createError) {
      console.error('Create error:', createError)
      return new Response(
        JSON.stringify({ error: 'Failed to create community' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    return new Response(
      JSON.stringify({
        success: true,
        community: {
          id: community.id,
          title: community.title,
          description: community.description,
          category: community.category,
          city: community.city,
          memberCount: community.member_count,
          createdBy: community.created_by,
          imageUrl: community.image_url,
          coverUrl: community.cover_url,
          rules: community.rules,
          createdAt: community.created_at,
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