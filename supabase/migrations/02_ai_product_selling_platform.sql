-- AI Product Selling Platform extensions
-- Safe additive migration. Does not delete or rewrite existing commerce data.

CREATE TABLE IF NOT EXISTS public.video_settings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  enabled BOOLEAN NOT NULL DEFAULT true,
  videos_per_day INTEGER NOT NULL DEFAULT 1 CHECK (videos_per_day BETWEEN 1 AND 100),
  generation_time TIME NOT NULL DEFAULT '10:00',
  timezone TEXT NOT NULL DEFAULT 'Asia/Kolkata',
  product_selection TEXT NOT NULL DEFAULT 'latest' CHECK (product_selection IN ('latest','bestseller','random','needs_promotion','selected')),
  require_admin_approval BOOLEAN NOT NULL DEFAULT true,
  default_aspect_ratio TEXT NOT NULL DEFAULT '9:16' CHECK (default_aspect_ratio IN ('9:16','16:9','1:1')),
  default_resolution TEXT NOT NULL DEFAULT '1080p',
  default_duration_seconds INTEGER NOT NULL DEFAULT 30 CHECK (default_duration_seconds BETWEEN 5 AND 180),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.video_jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID REFERENCES public.products(id) ON DELETE SET NULL,
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  title TEXT NOT NULL,
  description TEXT,
  video_url TEXT,
  thumbnail_url TEXT,
  preview_url TEXT,
  aspect_ratio TEXT NOT NULL DEFAULT '9:16' CHECK (aspect_ratio IN ('9:16','16:9','1:1')),
  resolution TEXT NOT NULL DEFAULT '1080p',
  duration_seconds INTEGER NOT NULL DEFAULT 30 CHECK (duration_seconds BETWEEN 5 AND 180),
  generation_status TEXT NOT NULL DEFAULT 'queued' CHECK (generation_status IN ('queued','processing','completed','failed','published','cancelled')),
  generation_provider TEXT,
  generation_prompt TEXT,
  scheduled_at TIMESTAMPTZ,
  generated_at TIMESTAMPTZ,
  published_at TIMESTAMPTZ,
  download_count INTEGER NOT NULL DEFAULT 0,
  view_count INTEGER NOT NULL DEFAULT 0,
  is_public BOOLEAN NOT NULL DEFAULT false,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.product_autopilot_settings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID UNIQUE NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  enabled BOOLEAN NOT NULL DEFAULT false,
  price_monitoring BOOLEAN NOT NULL DEFAULT true,
  stock_monitoring BOOLEAN NOT NULL DEFAULT true,
  ai_description BOOLEAN NOT NULL DEFAULT true,
  seo_generation BOOLEAN NOT NULL DEFAULT true,
  marketing_content BOOLEAN NOT NULL DEFAULT true,
  video_generation BOOLEAN NOT NULL DEFAULT false,
  low_margin_alert BOOLEAN NOT NULL DEFAULT true,
  supplier_price_alert BOOLEAN NOT NULL DEFAULT true,
  pause_when_out_of_stock BOOLEAN NOT NULL DEFAULT true,
  minimum_margin_percent NUMERIC(5,2) NOT NULL DEFAULT 10,
  updated_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.product_hunter_runs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  query TEXT,
  source TEXT NOT NULL,
  product_name TEXT,
  source_url TEXT,
  source_product_id TEXT,
  supplier_price NUMERIC(12,2),
  estimated_selling_price NUMERIC(12,2),
  estimated_profit NUMERIC(12,2),
  demand_score INTEGER CHECK (demand_score BETWEEN 0 AND 100),
  competition_score INTEGER CHECK (competition_score BETWEEN 0 AND 100),
  availability_score INTEGER CHECK (availability_score BETWEEN 0 AND 100),
  shipping_score INTEGER CHECK (shipping_score BETWEEN 0 AND 100),
  opportunity_score INTEGER CHECK (opportunity_score BETWEEN 0 AND 100),
  ai_reason TEXT,
  status TEXT NOT NULL DEFAULT 'researching' CHECK (status IN ('researching','shortlisted','pending_approval','approved','published','rejected')),
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.video_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.video_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_autopilot_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_hunter_runs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public published videos" ON public.video_jobs;
CREATE POLICY "Public published videos" ON public.video_jobs FOR SELECT
USING (is_public = true AND generation_status = 'published');

DROP POLICY IF EXISTS "Users can view own video jobs" ON public.video_jobs;
CREATE POLICY "Users can view own video jobs" ON public.video_jobs FOR SELECT
USING (created_by = auth.uid());

DROP POLICY IF EXISTS "Admins manage video jobs" ON public.video_jobs;
CREATE POLICY "Admins manage video jobs" ON public.video_jobs FOR ALL
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

DROP POLICY IF EXISTS "Admins manage video settings" ON public.video_settings;
CREATE POLICY "Admins manage video settings" ON public.video_settings FOR ALL
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

DROP POLICY IF EXISTS "Admins manage autopilot settings" ON public.product_autopilot_settings;
CREATE POLICY "Admins manage autopilot settings" ON public.product_autopilot_settings FOR ALL
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

DROP POLICY IF EXISTS "Admins manage product hunter" ON public.product_hunter_runs;
CREATE POLICY "Admins manage product hunter" ON public.product_hunter_runs FOR ALL
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

-- Automatic profile creation after customer/admin signup.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, email, full_name, role)
  VALUES (
    NEW.id,
    NEW.email,
    COALESCE(NEW.raw_user_meta_data ->> 'full_name', ''),
    'customer'
  )
  ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = CASE WHEN public.profiles.full_name IS NULL OR public.profiles.full_name = '' THEN EXCLUDED.full_name ELSE public.profiles.full_name END;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

INSERT INTO public.video_settings (enabled)
SELECT true
WHERE NOT EXISTS (SELECT 1 FROM public.video_settings);


-- The trigger is server-side only; do not expose it as a public RPC.
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM anon;
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM authenticated;
