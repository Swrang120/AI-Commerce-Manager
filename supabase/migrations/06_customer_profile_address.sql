-- Customer profile persistence: avatar + saved delivery address
create table if not exists public.customer_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  phone text,
  avatar_data text,
  address_line1 text,
  address_line2 text,
  landmark text,
  city text,
  state text,
  postal_code text,
  country text default 'India',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.customer_profiles enable row level security;

drop policy if exists "customer_profiles_select_own" on public.customer_profiles;
create policy "customer_profiles_select_own"
on public.customer_profiles for select
to authenticated
using (auth.uid() = id);

drop policy if exists "customer_profiles_insert_own" on public.customer_profiles;
create policy "customer_profiles_insert_own"
on public.customer_profiles for insert
to authenticated
with check (auth.uid() = id);

drop policy if exists "customer_profiles_update_own" on public.customer_profiles;
create policy "customer_profiles_update_own"
on public.customer_profiles for update
to authenticated
using (auth.uid() = id)
with check (auth.uid() = id);

create or replace function public.set_customer_profiles_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists customer_profiles_updated_at on public.customer_profiles;
create trigger customer_profiles_updated_at
before update on public.customer_profiles
for each row execute function public.set_customer_profiles_updated_at();

create index if not exists customer_profiles_postal_code_idx
on public.customer_profiles(postal_code);
