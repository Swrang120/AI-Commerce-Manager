-- AI Product Selling / Dropshipping Automation expansion
-- Safe additive migration: no existing rows are deleted or rewritten.

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
    resolution TEXT DEFAULT '1080p',
    duration_seconds INTEGER DEFAULT 30 CHECK (duration_seconds > 0 AND duration_seconds <= 180),
    generation_status TEXT NOT NULL DEFAULT 'queued' CHECK (generation_status IN ('queued','processing','completed','failed','cancelled')),
    generation_provider TEXT DEFAULT 'configured_provider',
    generation_prompt TEXT,
    scheduled_at TIMESTAMPTZ,
    generated_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    download_count INTEGER NOT NULL DEFAULT 0,
    view_count INTEGER NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);
CREATE INDEX IF NOT EXISTS idx_video_jobs_status_schedule ON public.video_jobs (generation_status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_video_jobs_product ON public.video_jobs (product_id);

CREATE TABLE IF NOT EXISTS public.video_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enabled BOOLEAN NOT NULL DEFAULT false,
    videos_per_day INTEGER NOT NULL DEFAULT 1 CHECK (videos_per_day BETWEEN 0 AND 50),
    generation_time TIME NOT NULL DEFAULT '10:00',
    timezone TEXT NOT NULL DEFAULT 'Asia/Kolkata',
    product_source TEXT NOT NULL DEFAULT 'latest' CHECK (product_source IN ('latest','bestseller','selected','random','needs_promotion')),
    default_aspect_ratio TEXT NOT NULL DEFAULT '9:16' CHECK (default_aspect_ratio IN ('9:16','16:9','1:1')),
    default_resolution TEXT NOT NULL DEFAULT '1080p',
    updated_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE TABLE IF NOT EXISTS public.product_autopilot_settings (
    product_id UUID PRIMARY KEY REFERENCES public.products(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT false,
    price_monitor BOOLEAN NOT NULL DEFAULT true,
    stock_monitor BOOLEAN NOT NULL DEFAULT true,
    ai_description BOOLEAN NOT NULL DEFAULT true,
    seo_content BOOLEAN NOT NULL DEFAULT true,
    marketing_content BOOLEAN NOT NULL DEFAULT true,
    sales_analytics BOOLEAN NOT NULL DEFAULT true,
    low_margin_alert BOOLEAN NOT NULL DEFAULT true,
    supplier_price_alert BOOLEAN NOT NULL DEFAULT true,
    pause_when_out_of_stock BOOLEAN NOT NULL DEFAULT false,
    minimum_margin_percent NUMERIC(8,2) NOT NULL DEFAULT 10.00,
    updated_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE TABLE IF NOT EXISTS public.product_price_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID UNIQUE REFERENCES public.products(id) ON DELETE CASCADE,
    pricing_mode TEXT NOT NULL DEFAULT 'margin' CHECK (pricing_mode IN ('fixed_profit','margin','minimum_profit','manual')),
    fixed_profit NUMERIC(12,2) DEFAULT 0,
    target_margin_percent NUMERIC(8,2) DEFAULT 20,
    minimum_profit NUMERIC(12,2) DEFAULT 0,
    supplier_cost NUMERIC(12,2) DEFAULT 0,
    shipping_cost NUMERIC(12,2) DEFAULT 0,
    platform_fee_percent NUMERIC(8,2) DEFAULT 0,
    platform_fee_fixed NUMERIC(12,2) DEFAULT 0,
    maximum_discount_percent NUMERIC(8,2) DEFAULT 0,
    last_calculated_price NUMERIC(12,2),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE TABLE IF NOT EXISTS public.product_hunter_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query TEXT,
    source TEXT NOT NULL,
    source_url TEXT,
    product_name TEXT NOT NULL,
    supplier_price NUMERIC(12,2),
    estimated_selling_price NUMERIC(12,2),
    estimated_profit NUMERIC(12,2),
    demand_score INTEGER CHECK (demand_score BETWEEN 0 AND 100),
    competition_score INTEGER CHECK (competition_score BETWEEN 0 AND 100),
    availability_score INTEGER CHECK (availability_score BETWEEN 0 AND 100),
    shipping_score INTEGER CHECK (shipping_score BETWEEN 0 AND 100),
    opportunity_score INTEGER CHECK (opportunity_score BETWEEN 0 AND 100),
    ai_reason TEXT,
    status TEXT NOT NULL DEFAULT 'researching' CHECK (status IN ('researching','shortlisted','approved','rejected','imported')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.video_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.video_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_autopilot_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_price_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_hunter_runs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public can view public videos" ON public.video_jobs;
CREATE POLICY "Public can view public videos" ON public.video_jobs FOR SELECT TO anon, authenticated
USING (is_public = true AND generation_status = 'completed');

DROP POLICY IF EXISTS "Admins manage video jobs" ON public.video_jobs;
CREATE POLICY "Admins manage video jobs" ON public.video_jobs FOR ALL TO authenticated
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')))
WITH CHECK (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

DROP POLICY IF EXISTS "Admins manage video settings" ON public.video_settings;
CREATE POLICY "Admins manage video settings" ON public.video_settings FOR ALL TO authenticated
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')))
WITH CHECK (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

DROP POLICY IF EXISTS "Admins manage autopilot settings" ON public.product_autopilot_settings;
CREATE POLICY "Admins manage autopilot settings" ON public.product_autopilot_settings FOR ALL TO authenticated
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')))
WITH CHECK (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

DROP POLICY IF EXISTS "Admins manage price rules" ON public.product_price_rules;
CREATE POLICY "Admins manage price rules" ON public.product_price_rules FOR ALL TO authenticated
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')))
WITH CHECK (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

DROP POLICY IF EXISTS "Admins manage hunter runs" ON public.product_hunter_runs;
CREATE POLICY "Admins manage hunter runs" ON public.product_hunter_runs FOR ALL TO authenticated
USING (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')))
WITH CHECK (EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid() AND p.role IN ('admin','manager')));

INSERT INTO public.video_settings (id)
SELECT gen_random_uuid() WHERE NOT EXISTS (SELECT 1 FROM public.video_settings);

COMMENT ON TABLE public.video_jobs IS 'AI product video queue and library; provider calls must be server-side.';
COMMENT ON TABLE public.product_autopilot_settings IS 'Per-product AI automation controls. External posting/ordering requires an approved API integration.';
COMMENT ON TABLE public.product_price_rules IS 'Transparent dropshipping pricing formula inputs and calculated selling price.';
COMMENT ON TABLE public.product_hunter_runs IS 'AI product discovery candidates awaiting admin approval.';
