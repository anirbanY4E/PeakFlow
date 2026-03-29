-- ============================================
-- PeakFlow Events Performance Optimization
-- Description: Add RPC functions to eliminate N+1 queries on events screens
-- ============================================

-- ============================================
-- FUNCTION: Get user's events with RSVP status (replaces N+1 in EventsListComponent)
-- Returns all events from communities the user belongs to, with RSVP status
-- ============================================

CREATE OR REPLACE FUNCTION public.get_user_events_with_rsvp(
  p_user_id UUID,
  p_category event_category DEFAULT NULL
)
RETURNS TABLE (
  id UUID,
  community_id UUID,
  community_name TEXT,
  title TEXT,
  description TEXT,
  category event_category,
  event_date DATE,
  event_time TEXT,
  end_time TEXT,
  location TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  image_url TEXT,
  max_participants INT,
  current_participants INT,
  is_free BOOLEAN,
  price DECIMAL,
  created_at TIMESTAMPTZ,
  is_rsvped BOOLEAN
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
    e.date AS event_date,
    e.time AS event_time,
    e.end_time,
    e.location,
    e.latitude,
    e.longitude,
    e.image_url,
    e.max_participants,
    e.current_participants,
    e.is_free,
    e.price,
    e.created_at,
    EXISTS (
      SELECT 1 FROM public.rsvps r
      WHERE r.event_id = e.id AND r.user_id = p_user_id
    ) AS is_rsvped
  FROM public.events e
  INNER JOIN public.memberships m ON e.community_id = m.community_id
  INNER JOIN public.communities c ON e.community_id = c.id
  WHERE m.user_id = p_user_id
    AND m.role != 'PENDING'::membership_role
    AND (p_category IS NULL OR e.category = p_category)
  ORDER BY e.date ASC, e.time ASC;
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================
-- FUNCTION: Get event detail with community, RSVP, and check-in (replaces 4 sequential calls)
-- ============================================

CREATE OR REPLACE FUNCTION public.get_event_detail(
  p_event_id UUID,
  p_user_id UUID
)
RETURNS TABLE (
  id UUID,
  community_id UUID,
  community_name TEXT,
  community_image TEXT,
  title TEXT,
  description TEXT,
  category event_category,
  event_date DATE,
  event_time TEXT,
  end_time TEXT,
  location TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  image_url TEXT,
  max_participants INT,
  current_participants INT,
  is_free BOOLEAN,
  price DECIMAL,
  created_at TIMESTAMPTZ,
  is_rsvped BOOLEAN,
  is_checked_in BOOLEAN
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    e.id,
    e.community_id,
    c.title AS community_name,
    c.image_url AS community_image,
    e.title,
    e.description,
    e.category,
    e.date AS event_date,
    e.time AS event_time,
    e.end_time,
    e.location,
    e.latitude,
    e.longitude,
    e.image_url,
    e.max_participants,
    e.current_participants,
    e.is_free,
    e.price,
    e.created_at,
    EXISTS (
      SELECT 1 FROM public.rsvps r
      WHERE r.event_id = e.id AND r.user_id = p_user_id
    ) AS is_rsvped,
    EXISTS (
      SELECT 1 FROM public.attendances a
      WHERE a.event_id = e.id AND a.user_id = p_user_id
    ) AS is_checked_in
  FROM public.events e
  INNER JOIN public.communities c ON e.community_id = c.id
  WHERE e.id = p_event_id;
END;
$$ LANGUAGE plpgsql STABLE;
