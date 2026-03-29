-- ============================================
-- PeakFlow Performance Optimization Migration
-- Description: Optimize RLS policies and add missing FK indexes
-- ============================================

-- ============================================
-- FIX #5: Optimize RLS policies to use (select auth.uid())
-- This evaluates auth.uid() ONCE instead of per-row
-- ============================================

-- profiles
DROP POLICY IF EXISTS "profiles_insert_own" ON public.profiles;
CREATE POLICY "profiles_insert_own" ON public.profiles
  FOR INSERT WITH CHECK ((select auth.uid()) = id);

DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
CREATE POLICY "profiles_update_own" ON public.profiles
  FOR UPDATE USING ((select auth.uid()) = id);

-- communities
DROP POLICY IF EXISTS "communities_insert_authenticated" ON public.communities;
CREATE POLICY "communities_insert_authenticated" ON public.communities
  FOR INSERT WITH CHECK ((select auth.uid()) IS NOT NULL);

DROP POLICY IF EXISTS "communities_update_admin" ON public.communities;
CREATE POLICY "communities_update_admin" ON public.communities
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = (select auth.uid())
        AND community_id = communities.id
        AND role = 'ADMIN'
    )
  );

-- memberships
DROP POLICY IF EXISTS "memberships_insert_own" ON public.memberships;
CREATE POLICY "memberships_insert_own" ON public.memberships
  FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "memberships_delete_own" ON public.memberships;
CREATE POLICY "memberships_delete_own" ON public.memberships
  FOR DELETE USING ((select auth.uid()) = user_id);

-- invite_codes
DROP POLICY IF EXISTS "invite_codes_insert_admin" ON public.invite_codes;
CREATE POLICY "invite_codes_insert_admin" ON public.invite_codes
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = (select auth.uid())
        AND community_id = invite_codes.community_id
        AND role = 'ADMIN'
    )
  );

DROP POLICY IF EXISTS "invite_codes_update_admin" ON public.invite_codes;
CREATE POLICY "invite_codes_update_admin" ON public.invite_codes
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = (select auth.uid())
        AND m.community_id = invite_codes.community_id
        AND m.role = 'ADMIN'
    )
  );

-- join_requests
DROP POLICY IF EXISTS "join_requests_select_own_or_admin" ON public.join_requests;
CREATE POLICY "join_requests_select_own_or_admin" ON public.join_requests
  FOR SELECT USING (
    user_id = (select auth.uid())
    OR EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = (select auth.uid())
        AND m.community_id = join_requests.community_id
        AND m.role = 'ADMIN'
    )
  );

DROP POLICY IF EXISTS "join_requests_insert_own" ON public.join_requests;
CREATE POLICY "join_requests_insert_own" ON public.join_requests
  FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "join_requests_update_admin" ON public.join_requests;
CREATE POLICY "join_requests_update_admin" ON public.join_requests
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = (select auth.uid())
        AND m.community_id = join_requests.community_id
        AND m.role = 'ADMIN'
    )
  );

-- events
DROP POLICY IF EXISTS "events_insert_admin" ON public.events;
CREATE POLICY "events_insert_admin" ON public.events
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = (select auth.uid())
        AND community_id = events.community_id
        AND role = 'ADMIN'
    )
  );

DROP POLICY IF EXISTS "events_update_admin" ON public.events;
CREATE POLICY "events_update_admin" ON public.events
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = (select auth.uid())
        AND m.community_id = events.community_id
        AND m.role = 'ADMIN'
    )
  );

DROP POLICY IF EXISTS "events_delete_admin" ON public.events;
CREATE POLICY "events_delete_admin" ON public.events
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = (select auth.uid())
        AND m.community_id = events.community_id
        AND m.role = 'ADMIN'
    )
  );

-- posts
DROP POLICY IF EXISTS "posts_insert_member" ON public.posts;
CREATE POLICY "posts_insert_member" ON public.posts
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = (select auth.uid())
        AND community_id = posts.community_id
        AND role != 'PENDING'
    )
  );

DROP POLICY IF EXISTS "posts_delete_own_or_admin" ON public.posts;
CREATE POLICY "posts_delete_own_or_admin" ON public.posts
  FOR DELETE USING (
    author_id = (select auth.uid())
    OR EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = (select auth.uid())
        AND m.community_id = posts.community_id
        AND m.role = 'ADMIN'
    )
  );

-- post_likes
DROP POLICY IF EXISTS "post_likes_insert_own" ON public.post_likes;
CREATE POLICY "post_likes_insert_own" ON public.post_likes
  FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "post_likes_delete_own" ON public.post_likes;
CREATE POLICY "post_likes_delete_own" ON public.post_likes
  FOR DELETE USING ((select auth.uid()) = user_id);

-- comments
DROP POLICY IF EXISTS "comments_insert_member" ON public.comments;
CREATE POLICY "comments_insert_member" ON public.comments
  FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "comments_delete_own_or_admin" ON public.comments;
CREATE POLICY "comments_delete_own_or_admin" ON public.comments
  FOR DELETE USING (
    user_id = (select auth.uid())
    OR EXISTS (
      SELECT 1 FROM public.memberships m
      JOIN public.posts p ON p.community_id = m.community_id
      WHERE m.user_id = (select auth.uid())
        AND p.id = comments.post_id
        AND m.role = 'ADMIN'
    )
  );

-- rsvps
DROP POLICY IF EXISTS "rsvps_insert_member" ON public.rsvps;
CREATE POLICY "rsvps_insert_member" ON public.rsvps
  FOR INSERT WITH CHECK (
    (select auth.uid()) = user_id
    AND EXISTS (
      SELECT 1 FROM public.memberships m
      JOIN public.events e ON e.community_id = m.community_id
      WHERE m.user_id = (select auth.uid())
        AND e.id = rsvps.event_id
        AND m.role != 'PENDING'
    )
  );

DROP POLICY IF EXISTS "rsvps_delete_own" ON public.rsvps;
CREATE POLICY "rsvps_delete_own" ON public.rsvps
  FOR DELETE USING ((select auth.uid()) = user_id);

-- attendances
DROP POLICY IF EXISTS "attendances_insert_member" ON public.attendances;
CREATE POLICY "attendances_insert_member" ON public.attendances
  FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "attendances_delete_admin" ON public.attendances;
CREATE POLICY "attendances_delete_admin" ON public.attendances
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      JOIN public.events e ON e.community_id = m.community_id
      WHERE m.user_id = (select auth.uid())
        AND e.id = attendances.event_id
        AND m.role = 'ADMIN'
    )
  );

-- ============================================
-- FIX #6: Add missing foreign key indexes
-- ============================================

CREATE INDEX IF NOT EXISTS idx_invite_codes_created_by ON public.invite_codes(created_by);
CREATE INDEX IF NOT EXISTS idx_join_requests_reviewed_by ON public.join_requests(reviewed_by);
CREATE INDEX IF NOT EXISTS idx_memberships_invited_by ON public.memberships(invited_by);
