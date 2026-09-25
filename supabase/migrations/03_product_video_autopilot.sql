-- E-commerce Shoping: safe additive video automation/storage setup.
-- Does not delete or rewrite existing product/order data.

insert into storage.buckets (id, name, public)
values ('product-videos', 'product-videos', true)
on conflict (id) do update set public = true;

create policy "public product videos are readable"
on storage.objects for select
to public
using (bucket_id = 'product-videos');

-- Daily scheduler. Supabase must have pg_cron + pg_net enabled.
-- The URL and Authorization token are read from Supabase Vault, so no secret is stored in Git.
do $$
begin
  if exists (select 1 from pg_extension where extname = 'pg_cron')
     and exists (select 1 from pg_extension where extname = 'pg_net') then
    perform cron.unschedule('ecommerce-shoping-daily-product-videos')
      where exists (select 1 from cron.job where jobname = 'ecommerce-shoping-daily-product-videos');

    perform cron.schedule(
      'ecommerce-shoping-daily-product-videos',
      '30 3 * * *',
      $job$
        select net.http_post(
          url := (select decrypted_secret from vault.decrypted_secrets where name = 'SUPABASE_PROJECT_URL' limit 1) || '/functions/v1/daily-product-videos',
          headers := jsonb_build_object(
            'Content-Type','application/json',
            'Authorization','Bearer ' || (select decrypted_secret from vault.decrypted_secrets where name = 'SUPABASE_SERVICE_ROLE_KEY' limit 1)
          ),
          body := '{}'::jsonb
        );
      $job$
    );
  end if;
end $$;
