-- Add the Gemini/Veo provider operation identifier without touching existing product/order data.
alter table public.video_jobs
  add column if not exists provider_operation_id text;

create index if not exists video_jobs_provider_operation_idx
  on public.video_jobs (provider_operation_id)
  where provider_operation_id is not null;
