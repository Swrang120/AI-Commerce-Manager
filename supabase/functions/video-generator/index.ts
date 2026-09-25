import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const providerUrl = Deno.env.get("VIDEO_PROVIDER_URL");
const providerApiKey = Deno.env.get("VIDEO_PROVIDER_API_KEY");
const admin = createClient(supabaseUrl, serviceRoleKey, { auth: { persistSession: false, autoRefreshToken: false } });

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return new Response(JSON.stringify({ error: "POST required" }), { status: 405 });
  try {
    const auth = req.headers.get("Authorization");
    if (!auth) return new Response(JSON.stringify({ error: "Authorization required" }), { status: 401 });
    const token = auth.replace(/^Bearer\s+/i, "");
    const { data: userData } = await admin.auth.getUser(token);
    if (!userData.user) return new Response(JSON.stringify({ error: "Invalid session" }), { status: 401 });

    const { data: profile } = await admin.from("profiles").select("role").eq("id", userData.user.id).maybeSingle();
    if (!profile || !["admin", "manager"].includes(profile.role)) {
      return new Response(JSON.stringify({ error: "Admin access required" }), { status: 403 });
    }
    if (!providerUrl || !providerApiKey) return new Response(JSON.stringify({ error: "Video provider is not configured" }), { status: 503 });

    const body = await req.json();
    if (!body.job_id) return new Response(JSON.stringify({ error: "job_id is required" }), { status: 400 });
    const { data: job, error: jobError } = await admin.from("video_jobs").select("*").eq("id", body.job_id).single();
    if (jobError || !job) return new Response(JSON.stringify({ error: "Video job not found" }), { status: 404 });

    await admin.from("video_jobs").update({ generation_status: "processing", error_message: null, updated_at: new Date().toISOString() }).eq("id", job.id);

    const providerResponse = await fetch(providerUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": "Bearer " + providerApiKey },
      body: JSON.stringify({
        job_id: job.id, title: job.title, description: job.description,
        prompt: job.generation_prompt, aspect_ratio: job.aspect_ratio,
        resolution: job.resolution, duration_seconds: job.duration_seconds,
        watermark: { text: "E-commerce Shoping", position: "bottom-right", required: true }
      })
    });

    const providerText = await providerResponse.text();
    if (!providerResponse.ok) {
      await admin.from("video_jobs").update({
        generation_status: "failed",
        error_message: "Provider HTTP " + providerResponse.status + ": " + providerText.slice(0, 500),
        updated_at: new Date().toISOString()
      }).eq("id", job.id);
      return new Response(JSON.stringify({ error: "Video provider failed" }), { status: 502 });
    }

    const result = JSON.parse(providerText);
    if (!result.video_url) {
      await admin.from("video_jobs").update({
        generation_status: "failed", error_message: "Provider response did not contain video_url.",
        updated_at: new Date().toISOString()
      }).eq("id", job.id);
      return new Response(JSON.stringify({ error: "Provider returned no video URL" }), { status: 502 });
    }

    await admin.from("video_jobs").update({
      video_url: result.video_url, thumbnail_url: result.thumbnail_url ?? null,
      preview_url: result.preview_url ?? result.video_url, generation_status: "completed",
      generated_at: new Date().toISOString(), updated_at: new Date().toISOString()
    }).eq("id", job.id);

    return new Response(JSON.stringify({ ok: true, job_id: job.id, video_url: result.video_url }), {
      headers: { "Content-Type": "application/json" }
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: error instanceof Error ? error.message : "Unexpected error" }), { status: 500 });
  }
});
