-- ============================================
-- PeakFlow Pagination && Compound Indexes
-- Implements offset pagination and high-cardinality indexes
-- ============================================

CREATE OR REPLACE FUNCTION public.get_community_posts(
  p_community_id UUID,
  p_limit INT DEFAULT 20,
  p_offset INT DEFAULT 0
)
RETURNS TABLE (
  id UUID,
  author_id UUID,
  author_name TEXT,
  author_avatar_url TEXT,
  content TEXT,
  image_url TEXT,
  likes_count INT,
  comments_count INT,
  created_at TIMESTAMPTZ,
  is_liked BOOLEAN
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    p.id,
    p.author_id,
    pr.name AS author_name,
    pr.avatar_url AS author_avatar_url,
    p.content,
    p.image_url,
    p.likes_count,
    p.comments_count,
    p.created_at,
    EXISTS (
      SELECT 1 FROM public.post_likes pl
      WHERE pl.post_id = p.id AND pl.user_id = auth.uid()
    ) AS is_liked
  FROM public.posts p
  LEFT JOIN public.profiles pr ON p.author_id = pr.id
  WHERE p.community_id = p_community_id
  ORDER BY p.created_at DESC
  LIMIT p_limit
  OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION public.get_user_feed(
  p_user_id UUID, 
  p_limit INT DEFAULT 50,
  p_offset INT DEFAULT 0
)
RETURNS TABLE (
  id UUID,
  community_id UUID,
  community_name TEXT,
  community_image TEXT,
  author_id UUID,
  author_name TEXT,
  author_avatar_url TEXT,
  content TEXT,
  image_url TEXT,
  likes_count INT,
  comments_count INT,
  created_at TIMESTAMPTZ,
  is_liked BOOLEAN
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    p.id,
    p.community_id,
    c.title AS community_name,
    c.image_url AS community_image,
    p.author_id,
    pr.name AS author_name,
    pr.avatar_url AS author_avatar_url,
    p.content,
    p.image_url,
    p.likes_count,
    p.comments_count,
    p.created_at,
    EXISTS (
      SELECT 1 FROM public.post_likes pl
      WHERE pl.post_id = p.id AND pl.user_id = p_user_id
    ) AS is_liked
  FROM public.posts p
  INNER JOIN public.memberships m ON p.community_id = m.community_id
  INNER JOIN public.communities c ON p.community_id = c.id
  LEFT JOIN public.profiles pr ON p.author_id = pr.id
  WHERE m.user_id = p_user_id AND m.role != 'PENDING'::membership_role
  ORDER BY p.created_at DESC
  LIMIT p_limit
  OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

-- Compound Indexes
CREATE INDEX IF NOT EXISTS idx_memberships_user_community ON public.memberships(user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_posts_community_created ON public.posts(community_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_post_created ON public.comments(post_id, created_at DESC);
