-- ============================================
-- PeakFlow Discover Communities Optimization
-- Eliminates N+1 membership fetch + inefficient neq filters
-- ============================================

CREATE OR REPLACE FUNCTION public.get_discover_communities(
  p_user_id UUID,
  p_city TEXT DEFAULT '',
  p_limit INT DEFAULT 50
)
RETURNS TABLE (
  id UUID,
  title TEXT,
  description TEXT,
  city TEXT,
  category event_category,
  member_count INT,
  image_url TEXT,
  cover_url TEXT,
  rules TEXT[],
  created_at TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    c.id,
    c.title,
    c.description,
    c.city,
    c.category,
    c.member_count,
    c.image_url,
    c.cover_url,
    c.rules,
    c.created_at
  FROM public.communities c
  WHERE 
    NOT EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = p_user_id 
        AND m.community_id = c.id
        AND m.role != 'PENDING'::membership_role
    )
    AND (p_city = '' OR c.city ILIKE '%' || p_city || '%')
  ORDER BY c.member_count DESC, c.created_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE INDEX IF NOT EXISTS idx_communities_city_member_count 
ON public.communities(city, member_count DESC);

GRANT EXECUTE ON FUNCTION public.get_discover_communities(UUID, TEXT, INT) TO authenticated;
