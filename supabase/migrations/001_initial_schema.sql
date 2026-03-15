-- ============================================
-- PeakFlow Database Schema
-- Version: 1.0.0
-- Description: Initial schema for fitness community platform
-- ============================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUMS
-- ============================================

CREATE TYPE event_category AS ENUM (
  'RUNNING',
  'CALISTHENICS',
  'TREKKING',
  'CYCLING',
  'KAYAKING',
  'ROCK_CLIMBING',
  'YOGA',
  'CROSSFIT',
  'SWIMMING',
  'ADVENTURE_SPORTS',
  'OTHER'
);

CREATE TYPE membership_role AS ENUM ('ADMIN', 'MEMBER', 'PENDING');

CREATE TYPE request_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- ============================================
-- USERS (extends Supabase auth.users)
-- ============================================

CREATE TABLE public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL DEFAULT '',
  email TEXT,
  phone TEXT,
  city TEXT NOT NULL DEFAULT '',
  avatar_url TEXT,
  interests event_category[] DEFAULT '{}',
  is_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "profiles_select_all" ON public.profiles
  FOR SELECT USING (TRUE);

CREATE POLICY "profiles_insert_own" ON public.profiles
  FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "profiles_update_own" ON public.profiles
  FOR UPDATE USING (auth.uid() = id);

-- Index for faster lookups
CREATE INDEX idx_profiles_email ON public.profiles(email);
CREATE INDEX idx_profiles_phone ON public.profiles(phone);

-- ============================================
-- COMMUNITIES
-- ============================================

CREATE TABLE public.communities (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  category event_category NOT NULL,
  city TEXT NOT NULL,
  member_count INT DEFAULT 0,
  created_by UUID NOT NULL REFERENCES public.profiles(id),
  image_url TEXT,
  cover_url TEXT,
  rules TEXT[] DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.communities ENABLE ROW LEVEL SECURITY;

-- Communities Policies
CREATE POLICY "communities_select_all" ON public.communities
  FOR SELECT USING (TRUE);

CREATE POLICY "communities_insert_authenticated" ON public.communities
  FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "communities_update_admin" ON public.communities
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = auth.uid()
        AND community_id = communities.id
        AND role = 'ADMIN'
    )
  );

-- Indexes
CREATE INDEX idx_communities_city ON public.communities(city);
CREATE INDEX idx_communities_category ON public.communities(category);
CREATE INDEX idx_communities_created_by ON public.communities(created_by);

-- ============================================
-- MEMBERSHIPS
-- ============================================

CREATE TABLE public.memberships (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  community_id UUID NOT NULL REFERENCES public.communities(id) ON DELETE CASCADE,
  role membership_role NOT NULL DEFAULT 'MEMBER',
  joined_at TIMESTAMPTZ DEFAULT NOW(),
  invited_by UUID REFERENCES public.profiles(id),
  UNIQUE(user_id, community_id)
);

ALTER TABLE public.memberships ENABLE ROW LEVEL SECURITY;

-- Memberships Policies
CREATE POLICY "memberships_select_all" ON public.memberships
  FOR SELECT USING (TRUE);

CREATE POLICY "memberships_insert_own" ON public.memberships
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "memberships_delete_own" ON public.memberships
  FOR DELETE USING (auth.uid() = user_id);

-- Indexes
CREATE INDEX idx_memberships_user ON public.memberships(user_id);
CREATE INDEX idx_memberships_community ON public.memberships(community_id);
CREATE INDEX idx_memberships_role ON public.memberships(role);

-- ============================================
-- INVITE CODES
-- ============================================

CREATE TABLE public.invite_codes (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  code TEXT NOT NULL UNIQUE,
  community_id UUID NOT NULL REFERENCES public.communities(id) ON DELETE CASCADE,
  created_by UUID NOT NULL REFERENCES public.profiles(id),
  max_uses INT,
  current_uses INT DEFAULT 0,
  expires_at TIMESTAMPTZ,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.invite_codes ENABLE ROW LEVEL SECURITY;

-- Invite Codes Policies
CREATE POLICY "invite_codes_select_all" ON public.invite_codes
  FOR SELECT USING (TRUE);

CREATE POLICY "invite_codes_insert_admin" ON public.invite_codes
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = auth.uid()
        AND community_id = invite_codes.community_id
        AND role = 'ADMIN'
    )
  );

CREATE POLICY "invite_codes_update_admin" ON public.invite_codes
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = auth.uid()
        AND m.community_id = invite_codes.community_id
        AND m.role = 'ADMIN'
    )
  );

-- Indexes
CREATE INDEX idx_invite_codes_code ON public.invite_codes(code);
CREATE INDEX idx_invite_codes_community ON public.invite_codes(community_id);
CREATE INDEX idx_invite_codes_active ON public.invite_codes(is_active);

-- ============================================
-- JOIN REQUESTS
-- ============================================

CREATE TABLE public.join_requests (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  community_id UUID NOT NULL REFERENCES public.communities(id) ON DELETE CASCADE,
  status request_status NOT NULL DEFAULT 'PENDING',
  requested_at TIMESTAMPTZ DEFAULT NOW(),
  reviewed_at TIMESTAMPTZ,
  reviewed_by UUID REFERENCES public.profiles(id),
  UNIQUE(user_id, community_id)
);

ALTER TABLE public.join_requests ENABLE ROW LEVEL SECURITY;

-- Join Requests Policies
CREATE POLICY "join_requests_select_own_or_admin" ON public.join_requests
  FOR SELECT USING (
    user_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = auth.uid()
        AND m.community_id = join_requests.community_id
        AND m.role = 'ADMIN'
    )
  );

CREATE POLICY "join_requests_insert_own" ON public.join_requests
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "join_requests_update_admin" ON public.join_requests
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = auth.uid()
        AND m.community_id = join_requests.community_id
        AND m.role = 'ADMIN'
    )
  );

-- Indexes
CREATE INDEX idx_join_requests_user ON public.join_requests(user_id);
CREATE INDEX idx_join_requests_community ON public.join_requests(community_id);
CREATE INDEX idx_join_requests_status ON public.join_requests(status);

-- ============================================
-- EVENTS
-- ============================================

CREATE TABLE public.events (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  community_id UUID NOT NULL REFERENCES public.communities(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  category event_category NOT NULL,
  date DATE NOT NULL,
  time TEXT NOT NULL,
  end_time TEXT,
  location TEXT NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  image_url TEXT,
  max_participants INT NOT NULL,
  current_participants INT DEFAULT 0,
  is_free BOOLEAN DEFAULT TRUE,
  price DECIMAL(10,2),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;

-- Events Policies
CREATE POLICY "events_select_all" ON public.events
  FOR SELECT USING (TRUE);

CREATE POLICY "events_insert_admin" ON public.events
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = auth.uid()
        AND community_id = events.community_id
        AND role = 'ADMIN'
    )
  );

CREATE POLICY "events_update_admin" ON public.events
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = auth.uid()
        AND m.community_id = events.community_id
        AND m.role = 'ADMIN'
    )
  );

CREATE POLICY "events_delete_admin" ON public.events
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = auth.uid()
        AND m.community_id = events.community_id
        AND m.role = 'ADMIN'
    )
  );

-- Indexes
CREATE INDEX idx_events_community ON public.events(community_id);
CREATE INDEX idx_events_date ON public.events(date);
CREATE INDEX idx_events_category ON public.events(category);

-- ============================================
-- POSTS
-- ============================================

CREATE TABLE public.posts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  community_id UUID NOT NULL REFERENCES public.communities(id) ON DELETE CASCADE,
  author_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  image_url TEXT,
  likes_count INT DEFAULT 0,
  comments_count INT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;

-- Posts Policies
CREATE POLICY "posts_select_all" ON public.posts
  FOR SELECT USING (TRUE);

CREATE POLICY "posts_insert_member" ON public.posts
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.memberships
      WHERE user_id = auth.uid()
        AND community_id = posts.community_id
        AND role != 'PENDING'
    )
  );

CREATE POLICY "posts_delete_own_or_admin" ON public.posts
  FOR DELETE USING (
    author_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM public.memberships m
      WHERE m.user_id = auth.uid()
        AND m.community_id = posts.community_id
        AND m.role = 'ADMIN'
    )
  );

-- Indexes
CREATE INDEX idx_posts_community ON public.posts(community_id);
CREATE INDEX idx_posts_author ON public.posts(author_id);
CREATE INDEX idx_posts_created_at ON public.posts(created_at DESC);

-- ============================================
-- POST LIKES
-- ============================================

CREATE TABLE public.post_likes (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(post_id, user_id)
);

ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;

-- Post Likes Policies
CREATE POLICY "post_likes_select_all" ON public.post_likes
  FOR SELECT USING (TRUE);

CREATE POLICY "post_likes_insert_own" ON public.post_likes
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "post_likes_delete_own" ON public.post_likes
  FOR DELETE USING (auth.uid() = user_id);

-- Indexes
CREATE INDEX idx_post_likes_post ON public.post_likes(post_id);
CREATE INDEX idx_post_likes_user ON public.post_likes(user_id);

-- ============================================
-- COMMENTS
-- ============================================

CREATE TABLE public.comments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  likes_count INT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;

-- Comments Policies
CREATE POLICY "comments_select_all" ON public.comments
  FOR SELECT USING (TRUE);

CREATE POLICY "comments_insert_member" ON public.comments
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "comments_delete_own_or_admin" ON public.comments
  FOR DELETE USING (
    user_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM public.memberships m
      JOIN public.posts p ON p.community_id = m.community_id
      WHERE m.user_id = auth.uid()
        AND p.id = comments.post_id
        AND m.role = 'ADMIN'
    )
  );

-- Indexes
CREATE INDEX idx_comments_post ON public.comments(post_id);
CREATE INDEX idx_comments_user ON public.comments(user_id);

-- ============================================
-- RSVPS
-- ============================================

CREATE TABLE public.rsvps (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  event_id UUID NOT NULL REFERENCES public.events(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, event_id)
);

ALTER TABLE public.rsvps ENABLE ROW LEVEL SECURITY;

-- RSVPs Policies
CREATE POLICY "rsvps_select_all" ON public.rsvps
  FOR SELECT USING (TRUE);

CREATE POLICY "rsvps_insert_member" ON public.rsvps
  FOR INSERT WITH CHECK (
    auth.uid() = user_id
    AND EXISTS (
      SELECT 1 FROM public.memberships m
      JOIN public.events e ON e.community_id = m.community_id
      WHERE m.user_id = auth.uid()
        AND e.id = rsvps.event_id
        AND m.role != 'PENDING'
    )
  );

CREATE POLICY "rsvps_delete_own" ON public.rsvps
  FOR DELETE USING (auth.uid() = user_id);

-- Indexes
CREATE INDEX idx_rsvps_user ON public.rsvps(user_id);
CREATE INDEX idx_rsvps_event ON public.rsvps(event_id);

-- ============================================
-- ATTENDANCE (Check-ins)
-- ============================================

CREATE TABLE public.attendances (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  event_id UUID NOT NULL REFERENCES public.events(id) ON DELETE CASCADE,
  checked_in_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, event_id)
);

ALTER TABLE public.attendances ENABLE ROW LEVEL SECURITY;

-- Attendance Policies
CREATE POLICY "attendances_select_all" ON public.attendances
  FOR SELECT USING (TRUE);

CREATE POLICY "attendances_insert_member" ON public.attendances
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "attendances_delete_admin" ON public.attendances
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.memberships m
      JOIN public.events e ON e.community_id = m.community_id
      WHERE m.user_id = auth.uid()
        AND e.id = attendances.event_id
        AND m.role = 'ADMIN'
    )
  );

-- Indexes
CREATE INDEX idx_attendances_user ON public.attendances(user_id);
CREATE INDEX idx_attendances_event ON public.attendances(event_id);

-- ============================================
-- TRIGGER: Auto-update timestamps
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_profiles_updated_at
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_communities_updated_at
  BEFORE UPDATE ON public.communities
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_posts_updated_at
  BEFORE UPDATE ON public.posts
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_events_updated_at
  BEFORE UPDATE ON public.events
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================
-- TRIGGER: Auto-update member count
-- ============================================

CREATE OR REPLACE FUNCTION update_community_member_count()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    UPDATE public.communities
    SET member_count = member_count + 1
    WHERE id = NEW.community_id;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    UPDATE public.communities
    SET member_count = GREATEST(member_count - 1, 0)
    WHERE id = OLD.community_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER membership_count_trigger
AFTER INSERT OR DELETE ON public.memberships
FOR EACH ROW
WHEN (NEW.role IS NULL OR NEW.role != 'PENDING'::membership_role OR OLD.role != 'PENDING'::membership_role)
EXECUTE FUNCTION update_community_member_count();

-- ============================================
-- TRIGGER: Auto-create admin membership on community creation
-- ============================================

CREATE OR REPLACE FUNCTION create_admin_membership()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.memberships (user_id, community_id, role, joined_at)
  VALUES (NEW.created_by, NEW.id, 'ADMIN', NOW());
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER create_community_admin
AFTER INSERT ON public.communities
FOR EACH ROW EXECUTE FUNCTION create_admin_membership();

-- ============================================
-- TRIGGER: Update post likes count
-- ============================================

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
$$ LANGUAGE plpgsql;

CREATE TRIGGER post_likes_count_trigger
AFTER INSERT OR DELETE ON public.post_likes
FOR EACH ROW EXECUTE FUNCTION update_post_likes_count();

-- ============================================
-- TRIGGER: Update post comments count
-- ============================================

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
$$ LANGUAGE plpgsql;

CREATE TRIGGER post_comments_count_trigger
AFTER INSERT OR DELETE ON public.comments
FOR EACH ROW EXECUTE FUNCTION update_post_comments_count();

-- ============================================
-- TRIGGER: Update event participant count
-- ============================================

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
$$ LANGUAGE plpgsql;

CREATE TRIGGER rsvp_count_trigger
AFTER INSERT OR DELETE ON public.rsvps
FOR EACH ROW EXECUTE FUNCTION update_event_participant_count();

-- ============================================
-- ENABLE REALTIME
-- ============================================

ALTER PUBLICATION supabase_realtime ADD TABLE public.posts;
ALTER PUBLICATION supabase_realtime ADD TABLE public.comments;
ALTER PUBLICATION supabase_realtime ADD TABLE public.post_likes;
ALTER PUBLICATION supabase_realtime ADD TABLE public.events;
ALTER PUBLICATION supabase_realtime ADD TABLE public.rsvps;
ALTER PUBLICATION supabase_realtime ADD TABLE public.memberships;
ALTER PUBLICATION supabase_realtime ADD TABLE public.join_requests;