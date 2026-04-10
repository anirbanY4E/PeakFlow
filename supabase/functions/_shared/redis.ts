import { Redis } from "https://esm.sh/@upstash/redis@1.28.0";

const url = Deno.env.get("UPSTASH_REDIS_REST_URL");
const token = Deno.env.get("UPSTASH_REDIS_REST_TOKEN");

// We only initialize if the variables exist to allow graceful degradation
export const redis = url && token ? new Redis({
  url: url,
  token: token,
}) : null;
