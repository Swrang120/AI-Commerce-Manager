-- YouTube automatic upload integration. Additive only; existing product/order/video data is preserved.
alter table public.video_jobs
  add column if not exists youtube_video_id text,
  add column if not exists youtube_url text,
  add column if not exists youtube_upload_status text default 'not_started',
  add column if not exists youtube_uploaded_at timestamptz,
  add column if not exists youtube_error text;

create index if not exists video_jobs_youtube_status_idx
  on public.video_jobs (youtube_upload_status);

create table if not exists public.youtube_connections (
  id uuid primary key default gen_random_uuid(),
  channel_id text,
  channel_title text,
  connected_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  status text not null default 'connected'
);

alter table public.youtube_connections enable row level security;

create table if not exists public.youtube_oauth_states (
  state text primary key,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null
);

alter table public.youtube_oauth_states enable row level security;

create or replace function public.youtube_store_refresh_token(p_token text)
returns void
language plpgsql
security definer
set search_path = public, vault
as $$
declare
  secret_id uuid;
begin
  select id into secret_id from vault.secrets where name = 'youtube_refresh_token' limit 1;
  if secret_id is null then
    perform vault.create_secret(p_token, 'youtube_refresh_token', 'OAuth refresh token for automatic YouTube uploads');
  else
    perform vault.update_secret(secret_id, p_token, 'youtube_refresh_token', 'OAuth refresh token for automatic YouTube uploads');
  end if;
end;
$$;

create or replace function public.youtube_get_refresh_token()
returns text
language sql
security definer
set search_path = public, vault
as $$
  select decrypted_secret from vault.decrypted_secrets
  where name = 'youtube_refresh_token' limit 1;
$$;

revoke all on function public.youtube_store_refresh_token(text) from public;
revoke all on function public.youtube_store_refresh_token(text) from anon;
revoke all on function public.youtube_store_refresh_token(text) from authenticated;
grant execute on function public.youtube_store_refresh_token(text) to service_role;

revoke all on function public.youtube_get_refresh_token() from public;
revoke all on function public.youtube_get_refresh_token() from anon;
revoke all on function public.youtube_get_refresh_token() from authenticated;
grant execute on function public.youtube_get_refresh_token() to service_role;
