import { createClient } from 'npm:@supabase/supabase-js@2'
import { JWT } from 'npm:google-auth-library@9'

// ---- Inline Rate Limiting (self-contained, no _shared dependency) ----
let rateLimiter: any = null;
try {
  const redisUrl = Deno.env.get("UPSTASH_REDIS_REST_URL");
  const redisToken = Deno.env.get("UPSTASH_REDIS_REST_TOKEN");
  if (redisUrl && redisToken) {
    const { Redis } = await import("https://esm.sh/@upstash/redis@1.28.0");
    const { Ratelimit } = await import("https://esm.sh/@upstash/ratelimit@1.0.0");
    const redis = new Redis({ url: redisUrl, token: redisToken });
    rateLimiter = new Ratelimit({
      redis,
      limiter: Ratelimit.slidingWindow(60, "1 m"), // 60 pushes per minute
      analytics: true,
    });
  }
} catch (e) {
  console.warn("Rate limiter init failed (degrading gracefully):", e);
}

interface PostRecord {
  id: string
  community_id: string
  author_id: string
  content: string
}

interface WebhookPayload {
  type: 'INSERT'
  table: string
  record: PostRecord
  schema: 'public'
}

const getAccessToken = ({ clientEmail, privateKey }: { clientEmail: string, privateKey: string }): Promise<string> => {
  return new Promise((resolve, reject) => {
    const jwtClient = new JWT({
      email: clientEmail,
      key: privateKey,
      scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
    })
    jwtClient.authorize((err, tokens) => {
      if (err) { reject(err); return; }
      resolve(tokens!.access_token!)
    })
  })
}

Deno.serve(async (req) => {
  const clientEmail = Deno.env.get('FIREBASE_CLIENT_EMAIL');
  const privateKeyRaw = Deno.env.get('FIREBASE_PRIVATE_KEY');
  const projectId = Deno.env.get('FIREBASE_PROJECT_ID');

  if (!clientEmail || !privateKeyRaw || !projectId) {
    return new Response(JSON.stringify({ error: "Firebase credentials missing" }), { status: 500 });
  }
  const privateKey = privateKeyRaw.replace(/\\n/g, '\n');

  let payload: WebhookPayload;
  try {
    payload = await req.json();
  } catch (e) {
    return new Response(JSON.stringify({ error: "Invalid payload" }), { status: 400 });
  }

  // Rate Limiting Check (fail-open: if Redis unavailable, allow through)
  if (rateLimiter) {
    try {
      const { success } = await rateLimiter.limit(payload.record.community_id);
      if (!success) {
        console.warn(`Rate limit exceeded for community ${payload.record.community_id} push notifications`);
        return new Response(
          JSON.stringify({ error: "Too many requests" }),
          { status: 429, headers: { 'Content-Type': 'application/json' } }
        );
      }
    } catch (e) {
      console.warn("Rate limit check failed (allowing through):", e);
    }
  }

  const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
  const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
  const supabase = createClient(supabaseUrl, supabaseKey);

  console.log(`Processing push for new post in community ${payload.record.community_id} by author ${payload.record.author_id}`);

  // Fetch members — use explicit FK name to disambiguate the two memberships->profiles FKs
  const { data, error } = await supabase
    .from('memberships')
    .select('user_id, profiles!memberships_user_id_fkey(fcm_token)')
    .eq('community_id', payload.record.community_id)
    .neq('user_id', payload.record.author_id);

  if (error) {
    console.error("Error fetching memberships", error);
    return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  }

  const tokensSet = new Set<string>();
  data?.forEach((m: any) => {
    if (m.profiles && m.profiles.fcm_token) {
        tokensSet.add(m.profiles.fcm_token);
    }
  });
  
  const tokens = Array.from(tokensSet);
  console.log(`Found ${tokens.length} tokens to notify.`);

  if (tokens.length === 0) {
     return new Response(JSON.stringify({ success: true, count: 0 }), { status: 200, headers: {'Content-Type': 'application/json'} });
  }

  let accessToken: string;
  try {
     accessToken = await getAccessToken({ clientEmail, privateKey });
  } catch(e) {
     console.error("Error obtaining access token", e);
     return new Response(JSON.stringify({ error: "Auth failed" }), { status: 500 });
  }

  // Batch requests in chunks of 20
  const chunkSize = 20;
  let successes = 0;
  
  for (let i = 0; i < tokens.length; i += chunkSize) {
    const chunk = tokens.slice(i, i + chunkSize);
    const requests = chunk.map(fcmToken => fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({
          message: {
            token: fcmToken,
            notification: {
              title: 'New Post in Community!',
              body: payload.record.content.substring(0, 100) + (payload.record.content.length > 100 ? '...' : ''),
            },
            data: {
              community_id: payload.record.community_id,
              post_id: payload.record.id
            }
          },
        }),
      }
    ));
    
    const results = await Promise.allSettled(requests);
    results.forEach(r => { if (r.status === 'fulfilled' && r.value.ok) { successes++; } });
  }

  return new Response(JSON.stringify({ success: true, sentCount: successes, totalTokens: tokens.length }), {
    headers: { 'Content-Type': 'application/json' },
  });
})
