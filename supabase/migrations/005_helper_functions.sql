-- ============================================
-- PeakFlow Helper Functions
-- Version: 1.1.0
-- Description: Utility functions for common queries
-- ============================================

-- ============================================
-- FUNCTION: Get user's communities with membership role
-- ============================================

CREATE OR REPLACE FUNCTION public.get_user_communities(p_user_id UUID)
RETURNS TABLE (
  id UUID,
  title TEXT,
  description TEXT,
  category event_category,
  city TEXT,
  member_count INT,
  image_url TEXT,
  cover_url TEXT,
  created_at TIMESTAMPTZ,
  role membership_role,
  joined_at TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    c.id,
    c.title,
    c.description,
    c.category,
    c.city,
    c.member_count,
    c.image_url,
    c.cover_url,
    c.created_at,
    m.role,
    m.joined_at
  FROM public.communities c
  INNER JOIN public.memberships m ON c.id = m.community_id
  WHERE m.user_id = p_user_id AND m.role != 'PENDING'::membership_role
  ORDER BY m.joined_at DESC;
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================
-- FUNCTION: Get community posts with author info
-- ============================================

CREATE OR REPLACE FUNCTION public.get_community_posts(p_community_id UUID)
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
  ORDER BY p.created_at DESC;
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================
-- FUNCTION: Get user feed (posts from joined communities)
-- ============================================

CREATE OR REPLACE FUNCTION public.get_user_feed(p_user_id UUID, p_limit INT DEFAULT 50)
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
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================
-- FUNCTION: Get upcoming events for user
-- ============================================

CREATE OR REPLACE FUNCTION public.get_user_upcoming_events(p_user_id UUID)
RETURNS TABLE (
  id UUID,
  community_id UUID,
  community_name TEXT,
  title TEXT,
  description TEXT,
  category event_category,
  date DATE,
  time TEXT,
  end_time TEXT,
  location TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  image_url TEXT,
  max_participants INT,
  current_participants INT,
  is_free BOOLEAN,
  price DECIMAL,
  has_rsvp BOOLEAN
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    e.id,
    e.community_id,
    c.title AS community_name,
    e.title,
    e.description,
    e.category,
    e.date,
    e.time,
    e.end_time,
    e.location,
    e.latitude,
    e.longitude,
    e.image_url,
    e.max_participants,
    e.current_participants,
    e.is_free,
    e.price,
    EXISTS (
      SELECT 1 FROM public.rsvps r
      WHERE r.event_id = e.id AND r.user_id = p_user_id
    ) AS has_rsvp
  FROM public.events e
  INNER JOIN public.memberships m ON e.community_id = m.community_id
  INNER JOIN public.communities c ON e.community_id = c.id
  WHERE m.user_id = p_user_id
    AND m.role != 'PENDING'::membership_role
    AND e.date >= CURRENT_DATE
  ORDER BY e.date, e.time;
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================
-- FUNCTION: Search communities
-- ============================================

CREATE OR REPLACE FUNCTION public.search_communities(
  p_query TEXT DEFAULT '',
  p_category event_category DEFAULT NULL,
  p_city TEXT DEFAULT '',
  p_limit INT DEFAULT 20
)
RETURNS TABLE (
  id UUID,
  title TEXT,
  description TEXT,
  category event_category,
  city TEXT,
  member_count INT,
  image_url TEXT,
  created_at TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    c.id,
    c.title,
    c.description,
    c.category,
    c.city,
    c.member_count,
    c.image_url,
    c.created_at
  FROM public.communities c
  WHERE
    (p_query = '' OR c.title ILIKE '%' || p_query || '%' OR c.description ILIKE '%' || p_query || '%')
    AND (p_category IS NULL OR c.category = p_category)
    AND (p_city = '' OR c.city ILIKE '%' || p_city || '%')
  ORDER BY c.member_count DESC, c.created_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================
-- FUNCTION: Get community stats
-- ============================================

CREATE OR REPLACE FUNCTION public.get_community_stats(p_community_id UUID)
RETURNS JSON AS $$
DECLARE
  v_member_count INT;
  v_event_count INT;
  v_post_count INT;
  v_active_events INT;
BEGIN
  SELECT COUNT(*) INTO v_member_count
  FROM public.memberships m
  WHERE m.community_id = p_community_id AND m.role != 'PENDING'::membership_role;

  SELECT COUNT(*) INTO v_event_count
  FROM public.events e
  WHERE e.community_id = p_community_id;

  SELECT COUNT(*) INTO v_post_count
  FROM public.posts p
  WHERE p.community_id = p_community_id;

  SELECT COUNT(*) INTO v_active_events
  FROM public.events e
  WHERE e.community_id = p_community_id AND e.date >= CURRENT_DATE;

  RETURN json_build_object(
    'member_count', v_member_count,
    'event_count', v_event_count,
    'post_count', v_post_count,
    'active_events', v_active_events
  );
END;
$$ LANGUAGE plpgsql STABLE;
