-- ====================================================================
-- NEXUS AI COMMERCE - AUTHORITATIVE PRODUCTION DATABASE SCHEMA
-- Compatible with Supabase PostgreSQL & Row Level Security (RLS)
-- ====================================================================

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    full_name TEXT,
    role TEXT DEFAULT 'customer' CHECK (role IN ('customer', 'admin', 'manager', 'staff')),
    avatar_url TEXT,
    phone TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Store Settings Table
CREATE TABLE IF NOT EXISTS public.store_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_name TEXT DEFAULT 'Nexus AI Commerce' NOT NULL,
    currency TEXT DEFAULT 'INR' NOT NULL,
    razorpay_enabled BOOLEAN DEFAULT true,
    razorpay_page_url TEXT DEFAULT 'https://razorpay.me/@santiramswargiary',
    razorpay_key_id TEXT,
    auto_stock_sync BOOLEAN DEFAULT true,
    auto_price_monitor BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. Categories Table
CREATE TABLE IF NOT EXISTS public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    slug TEXT UNIQUE NOT NULL,
    icon TEXT,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. Suppliers Table
CREATE TABLE IF NOT EXISTS public.suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    website TEXT,
    type TEXT DEFAULT 'Marketplace' CHECK (type IN ('Marketplace', 'Manufacturer', 'Wholesaler', 'Affiliate', 'Shopify', 'Manual', 'Other')),
    contact TEXT,
    api_status TEXT DEFAULT 'Not Configured',
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. Products Table
CREATE TABLE IF NOT EXISTS public.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    slug TEXT UNIQUE NOT NULL,
    sku TEXT UNIQUE,
    description TEXT,
    short_description TEXT,
    category_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    supplier_id UUID REFERENCES public.suppliers(id) ON DELETE SET NULL,
    cost_price NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    shipping_cost NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    desired_profit NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    selling_price NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    compare_at_price NUMERIC(12,2),
    profit_margin NUMERIC(5,2) DEFAULT 0.00,
    stock_quantity INTEGER DEFAULT 0 NOT NULL,
    status TEXT DEFAULT 'draft' CHECK (status IN ('active', 'draft', 'pending_review', 'out_of_stock', 'paused', 'archived')),
    images TEXT[] DEFAULT ARRAY[]::TEXT[],
    tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    seo_title TEXT,
    seo_description TEXT,
    ai_generated_content JSONB,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 6. Product Variants Table
CREATE TABLE IF NOT EXISTS public.product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    sku TEXT,
    price NUMERIC(12,2) NOT NULL,
    cost_price NUMERIC(12,2),
    stock_quantity INTEGER DEFAULT 0,
    options JSONB,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 7. Product Sources Table
CREATE TABLE IF NOT EXISTS public.product_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    source_name TEXT NOT NULL,
    source_url TEXT NOT NULL,
    external_product_id TEXT,
    supplier_price NUMERIC(12,2),
    currency TEXT DEFAULT 'INR',
    availability TEXT DEFAULT 'in_stock',
    raw_data JSONB,
    last_scraped_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 8. Price History Table
CREATE TABLE IF NOT EXISTS public.price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    old_price NUMERIC(12,2) NOT NULL,
    new_price NUMERIC(12,2) NOT NULL,
    source TEXT DEFAULT 'supplier_monitor',
    recorded_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 9. Stock History Table
CREATE TABLE IF NOT EXISTS public.stock_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    old_stock INTEGER NOT NULL,
    new_stock INTEGER NOT NULL,
    change_reason TEXT,
    recorded_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 10. Customers Table
CREATE TABLE IF NOT EXISTS public.customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    total_orders INTEGER DEFAULT 0,
    total_spent NUMERIC(12,2) DEFAULT 0.00,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 11. Customer Addresses Table
CREATE TABLE IF NOT EXISTS public.customer_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES public.customers(id) ON DELETE CASCADE,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    city TEXT NOT NULL,
    state TEXT NOT NULL,
    postal_code TEXT NOT NULL,
    country TEXT DEFAULT 'India',
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 12. Orders Table
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number TEXT UNIQUE NOT NULL,
    customer_id UUID REFERENCES public.customers(id) ON DELETE SET NULL,
    customer_name TEXT NOT NULL,
    customer_email TEXT NOT NULL,
    customer_phone TEXT,
    shipping_address JSONB NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    shipping_cost NUMERIC(12,2) DEFAULT 0.00,
    discount_amount NUMERIC(12,2) DEFAULT 0.00,
    tax_amount NUMERIC(12,2) DEFAULT 0.00,
    total_amount NUMERIC(12,2) NOT NULL,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'paid', 'processing', 'supplier_pending', 'supplier_ordered', 'shipped', 'delivered', 'cancelled', 'refunded', 'failed')),
    payment_status TEXT DEFAULT 'pending' CHECK (payment_status IN ('pending', 'authorized', 'captured', 'failed', 'refunded', 'partially_refunded')),
    payment_method TEXT DEFAULT 'Razorpay',
    razorpay_order_id TEXT,
    razorpay_payment_id TEXT,
    supplier_status TEXT DEFAULT 'pending',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 13. Order Items Table
CREATE TABLE IF NOT EXISTS public.order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES public.products(id) ON DELETE SET NULL,
    variant_id UUID REFERENCES public.product_variants(id) ON DELETE SET NULL,
    product_name TEXT NOT NULL,
    sku TEXT,
    unit_price NUMERIC(12,2) NOT NULL,
    cost_price NUMERIC(12,2) DEFAULT 0.00,
    quantity INTEGER NOT NULL,
    total_price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 14. Payments Table
CREATE TABLE IF NOT EXISTS public.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    razorpay_order_id TEXT,
    razorpay_payment_id TEXT UNIQUE,
    razorpay_signature TEXT,
    amount NUMERIC(12,2) NOT NULL,
    currency TEXT DEFAULT 'INR',
    status TEXT NOT NULL,
    method TEXT,
    raw_response JSONB,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 15. Payment Webhook Events Table
CREATE TABLE IF NOT EXISTS public.payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id TEXT UNIQUE NOT NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT false,
    received_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 16. Supplier Orders Table
CREATE TABLE IF NOT EXISTS public.supplier_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES public.orders(id) ON DELETE CASCADE,
    supplier_id UUID REFERENCES public.suppliers(id) ON DELETE SET NULL,
    supplier_order_number TEXT NOT NULL,
    supplier_amount NUMERIC(12,2) NOT NULL,
    courier TEXT,
    tracking_number TEXT,
    status TEXT DEFAULT 'ordered',
    placed_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 17. Shipments Table
CREATE TABLE IF NOT EXISTS public.shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    courier_name TEXT NOT NULL,
    tracking_number TEXT NOT NULL,
    tracking_url TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'label_created', 'shipped', 'in_transit', 'out_for_delivery', 'delivered', 'returned', 'cancelled')),
    shipped_at TIMESTAMPTZ,
    estimated_delivery DATE,
    delivered_at TIMESTAMPTZ,
    tracking_events JSONB DEFAULT '[]'::JSONB,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 18. Refunds Table
CREATE TABLE IF NOT EXISTS public.refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    payment_id UUID REFERENCES public.payments(id),
    refund_id TEXT UNIQUE,
    amount NUMERIC(12,2) NOT NULL,
    reason TEXT,
    status TEXT DEFAULT 'processed',
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 19. Profit Records Table
CREATE TABLE IF NOT EXISTS public.profit_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID UNIQUE REFERENCES public.orders(id) ON DELETE CASCADE,
    revenue NUMERIC(12,2) NOT NULL,
    product_cost NUMERIC(12,2) NOT NULL,
    shipping_cost NUMERIC(12,2) NOT NULL,
    payment_fee NUMERIC(12,2) DEFAULT 0.00,
    marketing_cost NUMERIC(12,2) DEFAULT 0.00,
    net_profit NUMERIC(12,2) NOT NULL,
    profit_margin NUMERIC(5,2) NOT NULL,
    recorded_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 20. Marketing Campaigns Table
CREATE TABLE IF NOT EXISTS public.marketing_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    product_id UUID REFERENCES public.products(id) ON DELETE SET NULL,
    platform TEXT NOT NULL,
    tracking_code TEXT UNIQUE NOT NULL,
    destination_url TEXT NOT NULL,
    clicks_count INTEGER DEFAULT 0,
    orders_count INTEGER DEFAULT 0,
    revenue_generated NUMERIC(12,2) DEFAULT 0.00,
    generated_copy TEXT,
    status TEXT DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 21. Marketing Posts Table
CREATE TABLE IF NOT EXISTS public.marketing_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID REFERENCES public.marketing_campaigns(id) ON DELETE CASCADE,
    platform TEXT NOT NULL,
    content TEXT NOT NULL,
    post_url TEXT,
    scheduled_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    status TEXT DEFAULT 'draft' CHECK (status IN ('draft', 'scheduled', 'published', 'archived')),
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 22. Link Clicks Table
CREATE TABLE IF NOT EXISTS public.link_clicks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID REFERENCES public.marketing_campaigns(id) ON DELETE CASCADE,
    tracking_code TEXT NOT NULL,
    referrer TEXT,
    user_agent TEXT,
    ip_hash TEXT,
    clicked_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 23. Automation Rules Table
CREATE TABLE IF NOT EXISTS public.automation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    trigger_config JSONB,
    action_config JSONB,
    enabled BOOLEAN DEFAULT true,
    last_run TIMESTAMPTZ,
    next_run TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 24. Automation Runs Table
CREATE TABLE IF NOT EXISTS public.automation_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID REFERENCES public.automation_rules(id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    log_output TEXT,
    ran_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 25. AI Tasks Table
CREATE TABLE IF NOT EXISTS public.ai_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_type TEXT NOT NULL,
    product_id UUID REFERENCES public.products(id) ON DELETE SET NULL,
    prompt TEXT NOT NULL,
    model TEXT DEFAULT 'gemini-3.5-flash',
    output JSONB,
    status TEXT DEFAULT 'completed',
    error TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 26. Product Research Table
CREATE TABLE IF NOT EXISTS public.product_research (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_name TEXT NOT NULL,
    source TEXT NOT NULL,
    source_price NUMERIC(12,2) NOT NULL,
    estimated_selling_price NUMERIC(12,2) NOT NULL,
    estimated_profit NUMERIC(12,2) NOT NULL,
    demand_score INTEGER DEFAULT 50,
    competition_score INTEGER DEFAULT 50,
    opportunity_score INTEGER DEFAULT 50,
    ai_recommendation TEXT,
    status TEXT DEFAULT 'researching' CHECK (status IN ('researching', 'shortlisted', 'approved', 'rejected', 'imported')),
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 27. Notifications Table
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_type TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 28. Activity Logs Table
CREATE TABLE IF NOT EXISTS public.activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id TEXT,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 29. Wishlists Table
CREATE TABLE IF NOT EXISTS public.wishlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES public.customers(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE(customer_id, product_id)
);

-- 30. Carts Table
CREATE TABLE IF NOT EXISTS public.carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES public.customers(id) ON DELETE CASCADE,
    session_token TEXT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 31. Cart Items Table
CREATE TABLE IF NOT EXISTS public.cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES public.carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    variant_id UUID REFERENCES public.product_variants(id) ON DELETE SET NULL,
    quantity INTEGER DEFAULT 1 NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 32. Coupons Table
CREATE TABLE IF NOT EXISTS public.coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT UNIQUE NOT NULL,
    discount_type TEXT DEFAULT 'percentage' CHECK (discount_type IN ('percentage', 'fixed')),
    discount_value NUMERIC(12,2) NOT NULL,
    minimum_order_amount NUMERIC(12,2) DEFAULT 0.00,
    maximum_discount NUMERIC(12,2),
    usage_limit INTEGER,
    used_count INTEGER DEFAULT 0,
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 33. Product Reviews Table
CREATE TABLE IF NOT EXISTS public.product_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES public.customers(id) ON DELETE SET NULL,
    order_id UUID REFERENCES public.orders(id) ON DELETE SET NULL,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5) NOT NULL,
    title TEXT,
    review TEXT NOT NULL,
    is_verified_purchase BOOLEAN DEFAULT false,
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable Row Level Security (RLS) on all public tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.suppliers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_variants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shipments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.marketing_campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.marketing_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.automation_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.automation_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_research ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wishlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.coupons ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_reviews ENABLE ROW LEVEL SECURITY;

-- Customer Read Policies (Public / Authenticated)
CREATE POLICY "Public products are viewable by everyone" ON public.products FOR SELECT USING (status = 'active');
CREATE POLICY "Public categories are viewable by everyone" ON public.categories FOR SELECT USING (true);
CREATE POLICY "Active coupons are viewable by everyone" ON public.coupons FOR SELECT USING (is_active = true);
CREATE POLICY "Published reviews are viewable by everyone" ON public.product_reviews FOR SELECT USING (is_published = true);
CREATE POLICY "Public store settings are viewable by everyone" ON public.store_settings FOR SELECT USING (true);
CREATE POLICY "Users can view own profile" ON public.profiles FOR SELECT USING (auth.uid() = id);

-- Admin & Manager Full Access Policies via Profiles Role Check
CREATE POLICY "Admins have full access to products" ON public.products FOR ALL USING (
    EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role IN ('admin', 'manager'))
);
CREATE POLICY "Admins have full access to suppliers" ON public.suppliers FOR ALL USING (
    EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role IN ('admin', 'manager'))
);
CREATE POLICY "Admins have full access to orders" ON public.orders FOR ALL USING (
    EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role IN ('admin', 'manager'))
);
CREATE POLICY "Admins have full access to campaigns" ON public.marketing_campaigns FOR ALL USING (
    EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role IN ('admin', 'manager'))
);
CREATE POLICY "Admins have full access to research" ON public.product_research FOR ALL USING (
    EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role IN ('admin', 'manager'))
);
