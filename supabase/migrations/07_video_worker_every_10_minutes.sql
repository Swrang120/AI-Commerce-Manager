-- Faster autonomous AI video worker.
-- Additive only: preserves existing products, orders and video jobs.
-- The worker finishes an in-progress Veo operation before starting another job.
do $$
begin
  if exists (select 1 from pg_extension where extname = 'pg_cron')
     and exists (select 1 from pg_extension where extname = 'pg_net') then
    perform cron.unschedule('ecommerce-shoping-daily-product-videos')
      where exists (select 1 from cron.job where jobname = 'ecommerce-shoping-daily-product-videos');

    perform cron.unschedule('ecommerce-shoping-video-worker')
      where exists (select 1 from cron.job where jobname = 'ecommerce-shoping-video-worker');

    perform cron.schedule(
      'ecommerce-shoping-video-worker',
      '*/10 * * * *',
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
