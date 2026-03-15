-- ============================================
-- PeakFlow Auth Triggers
-- Version: 1.0.0
-- Description: Auto-create profile on user signup
-- ============================================

-- ============================================
-- FUNCTION: Handle new user signup
-- Creates a profile record when a new user signs up
-- ============================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, name, email, phone, created_at)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', ''),
    NEW.email,
    NEW.phone,
    NOW()
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- TRIGGER: Create profile on user creation
-- ============================================

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================
-- FUNCTION: Handle Google OAuth sign-in
-- Updates profile with Google data if user exists
-- ============================================

CREATE OR REPLACE FUNCTION public.handle_google_signin()
RETURNS TRIGGER AS $$
BEGIN
  -- Update profile if user already exists
  UPDATE public.profiles
  SET
    name = COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', name),
    avatar_url = COALESCE(NEW.raw_user_meta_data->>'avatar_url', NEW.raw_user_meta_data->>'picture', avatar_url),
    email = COALESCE(NEW.email, email),
    is_verified = TRUE  -- Google users are auto-verified
  WHERE id = NEW.id;

  -- If no profile exists, create one (handled by handle_new_user trigger)
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- TRIGGER: Update profile on Google sign-in
-- ============================================

DROP TRIGGER IF EXISTS on_auth_user_updated ON auth.users;

CREATE TRIGGER on_auth_user_updated
  AFTER UPDATE ON auth.users
  FOR EACH ROW
  WHEN (OLD.raw_user_meta_data->>'provider' IS DISTINCT FROM NEW.raw_user_meta_data->>'provider'
        OR OLD.email IS DISTINCT FROM NEW.email)
  EXECUTE FUNCTION public.handle_google_signin();