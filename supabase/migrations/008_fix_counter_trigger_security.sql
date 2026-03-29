-- ============================================
-- Fix: Counter trigger functions blocked by RLS
-- ============================================
-- All counter trigger functions were SECURITY INVOKER, meaning they ran as the
-- calling user. Since tables have RLS enabled with restricted UPDATE policies
-- (no UPDATE on posts, admin-only UPDATE on events/communities), the triggers'
-- UPDATE statements were silently blocked.
-- 
-- Fix: Make all counter triggers SECURITY DEFINER so they bypass RLS.
-- These are trusted internal counter updates triggered by INSERT/DELETE on
-- child tables (post_likes, comments, rsvps, memberships).
-- ============================================

-- Post likes counter
CREATE OR REPLACE FUNCTION update_post_likes_count()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    UPDATE public.posts SET likes_count = likes_count + 1 WHERE id = NEW.post_id;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    UPDATE public.posts SET likes_count = GREATEST(likes_count - 1, 0) WHERE id = OLD.post_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Post comments counter
CREATE OR REPLACE FUNCTION update_post_comments_count()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    UPDATE public.posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    UPDATE public.posts SET comments_count = GREATEST(comments_count - 1, 0) WHERE id = OLD.post_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Event participant counter
CREATE OR REPLACE FUNCTION update_event_participant_count()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    UPDATE public.events SET current_participants = current_participants + 1 WHERE id = NEW.event_id;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    UPDATE public.events SET current_participants = GREATEST(current_participants - 1, 0) WHERE id = OLD.event_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Community member counter
CREATE OR REPLACE FUNCTION update_community_member_count()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    UPDATE public.communities SET member_count = member_count + 1 WHERE id = NEW.community_id;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    UPDATE public.communities SET member_count = GREATEST(member_count - 1, 0) WHERE id = OLD.community_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Recalculate stale post counts from actual data
UPDATE public.posts p SET
  likes_count = (SELECT COUNT(*) FROM public.post_likes pl WHERE pl.post_id = p.id),
  comments_count = (SELECT COUNT(*) FROM public.comments c WHERE c.post_id = p.id);
