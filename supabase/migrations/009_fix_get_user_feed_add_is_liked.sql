-- Fix: get_user_feed was missing the is_liked column entirely.
-- This caused the client to always see posts as NOT liked in the feed screen,
-- which broke the like toggle (it would always try to INSERT, hitting the unique constraint).

-- Must drop first since return type is changing (adding is_liked column)
DROP FUNCTION IF EXISTS public.get_user_feed(UUID, INT);

CREATE FUNCTION public.get_user_feed(
  p_user_id UUID,
  p_limit INT DEFAULT 50
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
)
LANGUAGE plpgsql STABLE AS $$
BEGIN
  RETURN QUERY
  SELECT p.id, p.community_id, c.title, c.image_url,
    p.author_id, pr.name, pr.avatar_url,
    p.content, p.image_url, p.likes_count, p.comments_count, p.created_at,
    EXISTS (
      SELECT 1 FROM public.post_likes pl
      WHERE pl.post_id = p.id AND pl.user_id = auth.uid()
    ) AS is_liked
  FROM public.posts p
  INNER JOIN public.memberships m ON p.community_id = m.community_id
  INNER JOIN public.communities c ON p.community_id = c.id
  LEFT JOIN public.profiles pr ON p.author_id = pr.id
  WHERE m.user_id = p_user_id AND m.role != 'PENDING'::membership_role
  ORDER BY p.created_at DESC
  LIMIT p_limit;
END;
$$;
