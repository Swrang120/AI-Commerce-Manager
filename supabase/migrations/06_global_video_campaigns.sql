-- Global multi-country / multi-language AI video campaigns.
-- Additive only: existing products, orders and video jobs are preserved.

alter table public.video_jobs
  add column if not exists campaign_name text,
  add column if not exists country_code text,
  add column if not exists language_code text,
  add column if not exists locale text,
  add column if not exists target_platform text default 'youtube',
  add column if not exists scheduled_timezone text default 'UTC',
  add column if not exists localized_title text,
  add column if not exists localized_description text;

create index if not exists video_jobs_campaign_idx on public.video_jobs (campaign_name);
create index if not exists video_jobs_locale_idx on public.video_jobs (country_code, language_code);
create index if not exists video_jobs_global_queue_idx on public.video_jobs (generation_status, country_code, language_code);

create table if not exists public.video_campaigns (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  product_id uuid references public.products(id) on delete set null,
  countries text[] not null default '{}',
  languages text[] not null default '{}',
  target_platform text not null default 'youtube',
  timezone text not null default 'UTC',
  status text not null default 'queued' check (status in ('queued','processing','completed','partial','failed','cancelled')),
  total_jobs integer not null default 0,
  completed_jobs integer not null default 0,
  failed_jobs integer not null default 0,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.video_campaigns enable row level security;
drop policy if exists "Admins manage video campaigns" on public.video_campaigns;
create policy "Admins manage video campaigns" on public.video_campaigns for all
using (exists (select 1 from public.profiles p where p.id=auth.uid() and p.role in ('admin','manager')));

alter table public.video_jobs add column if not exists campaign_id uuid references public.video_campaigns(id) on delete set null;
create index if not exists video_jobs_campaign_id_idx on public.video_jobs (campaign_id);
