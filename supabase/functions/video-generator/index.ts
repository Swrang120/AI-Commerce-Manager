import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { S3Client, PutObjectCommand } from "npm:@aws-sdk/client-s3";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const geminiKey = Deno.env.get("GEMINI_API_KEY");
const r2AccountId = Deno.env.get("R2_ACCOUNT_ID");
const r2AccessKeyId = Deno.env.get("R2_ACCESS_KEY_ID");
const r2SecretAccessKey = Deno.env.get("R2_SECRET_ACCESS_KEY");
const r2Bucket = Deno.env.get("R2_BUCKET") || "ecommerce-shoping-videos";
const r2PublicBaseUrl = (Deno.env.get("R2_PUBLIC_BASE_URL") || "").replace(/\/$/, "");
const admin = createClient(supabaseUrl, serviceRoleKey, { auth: { persistSession: false, autoRefreshToken: false } });

const MODEL = "veo-3.1-generate-preview";
const GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta";
const BRAND = "E-commerce Shoping";

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json" } });
}
function b64(bytes: Uint8Array) {
  let out = "";
  for (let i = 0; i < bytes.length; i += 0x8000) out += String.fromCharCode(...bytes.subarray(i, Math.min(i + 0x8000, bytes.length)));
  return btoa(out);
}
function r2Client() {
  if (!r2AccountId || !r2AccessKeyId || !r2SecretAccessKey || !r2PublicBaseUrl) throw new Error("R2 storage is not configured");
  return new S3Client({
    region: "auto",
    endpoint: `https://${r2AccountId}.r2.cloudflarestorage.com`,
    credentials: { accessKeyId: r2AccessKeyId, secretAccessKey: r2SecretAccessKey }
  });
}
async function imageFromProduct(product: any) {
  const images = Array.isArray(product.images) ? product.images : [];
  const url = images.find((x: any) => typeof x === "string" && /^https?:\/\//i.test(x));
  if (!url) return null;
  try {
    const r = await fetch(url);
    if (!r.ok) return null;
    const mimeType = (r.headers.get("content-type") || "image/jpeg").split(";")[0];
    if (!mimeType.startsWith("image/")) return null;
    const bytes = new Uint8Array(await r.arrayBuffer());
    if (bytes.length > 5_000_000) return null;
    return { mimeType, data: b64(bytes) };
  } catch { return null; }
}
async function startVeo(prompt: string, image: any) {
  if (!geminiKey) throw new Error("GEMINI_API_KEY is not configured");
  const instance: any = { prompt };
  if (image) instance.image = { inlineData: { mimeType: image.mimeType, data: image.data } };
  const r = await fetch(`${GEMINI_BASE}/models/${MODEL}:predictLongRunning`, {
    method: "POST",
    headers: { "x-goog-api-key": geminiKey, "Content-Type": "application/json" },
    body: JSON.stringify({ instances: [instance], parameters: { aspectRatio: "9:16", resolution: "1080p", numberOfVideos: 1 } })
  });
  const t = await r.text();
  if (!r.ok) throw new Error("Gemini HTTP " + r.status + ": " + t.slice(0, 500));
  const data = JSON.parse(t);
  if (!data.name) throw new Error("Gemini did not return an operation name");
  return data.name;
}
async function finishVeo(operation: string) {
  if (!geminiKey) throw new Error("GEMINI_API_KEY is not configured");
  const r = await fetch(`${GEMINI_BASE}/${operation}`, { headers: { "x-goog-api-key": geminiKey } });
  const t = await r.text();
  if (!r.ok) throw new Error("Gemini operation HTTP " + r.status + ": " + t.slice(0, 500));
  const data = JSON.parse(t);
  if (!data.done) return null;
  if (data.error) throw new Error(data.error.message || "Gemini video generation failed");
  const uri = data.response?.generateVideoResponse?.generatedSamples?.[0]?.video?.uri;
  if (!uri) throw new Error("Gemini completed without video URI");
  const video = await fetch(uri, { headers: { "x-goog-api-key": geminiKey } });
  if (!video.ok) throw new Error("Could not download Gemini video");
  return new Uint8Array(await video.arrayBuffer());
}
async function uploadR2(jobId: string, bytes: Uint8Array) {
  if (!r2AccountId || !r2AccessKeyId || !r2SecretAccessKey || !r2PublicBaseUrl) throw new Error("R2 storage is not configured");
  const key = `videos/${jobId}.mp4`;
  await r2Client().send(new PutObjectCommand({ Bucket: r2Bucket, Key: key, Body: bytes, ContentType: "video/mp4", CacheControl: "public, max-age=31536000" }));
  return `${r2PublicBaseUrl}/${key}`;
}
async function triggerYoutubeUpload(jobId: string) {
  if (!serviceRoleKey) return;
  try {
    await fetch(`${supabaseUrl}/functions/v1/youtube-upload`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": "Bearer " + serviceRoleKey },
      body: JSON.stringify({ job_id: jobId })
    });
  } catch {
    // YouTube upload is best-effort; the R2 video remains published if YouTube is not configured.
  }
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "POST required" }, 405);
  try {
    const auth = req.headers.get("Authorization");
    if (!auth) return json({ error: "Authorization required" }, 401);
    const token = auth.replace(/^Bearer\s+/i, "");
    const { data: userData } = await admin.auth.getUser(token);
    if (!userData.user) return json({ error: "Invalid session" }, 401);
    const { data: profile } = await admin.from("profiles").select("role").eq("id", userData.user.id).maybeSingle();
    if (!profile || !["admin", "manager"].includes(profile.role)) return json({ error: "Admin access required" }, 403);

    if (!geminiKey || !r2AccountId || !r2AccessKeyId || !r2SecretAccessKey || !r2PublicBaseUrl) {
      return json({ error: "Gemini/R2 storage is not configured" }, 503);
    }

    const body = await req.json();
    if (!body.job_id) return json({ error: "job_id is required" }, 400);
    const { data: job, error: jobError } = await admin.from("video_jobs").select("*,products(id,name,description,short_description,price,images)").eq("id", body.job_id).single();
    if (jobError || !job) return json({ error: "Video job not found" }, 404);

    if (job.generation_status === "queued") {
      const product = job.products;
      const prompt = job.generation_prompt || [
        "Create a premium e-commerce product promotion video.",
        "Use only verified product facts. Do not invent specifications, reviews, discounts, certifications, prices, or performance claims.",
        "Show the product clearly with smooth commercial camera motion.",
        "Vertical 9:16 social-commerce video. Native appropriate audio.",
        "Persistent watermark: " + BRAND + ", bottom-right.",
        "Product name: " + product.name,
        "Description: " + (product.description ?? product.short_description ?? ""),
        "Current price: " + String(product.price ?? "")
      ].join("\n");
      const operation = await startVeo(prompt, await imageFromProduct(product));
      await admin.from("video_jobs").update({
        generation_status: "processing",
        generation_provider: "google-veo-3.1",
        provider_operation_id: operation,
        duration_seconds: 8,
        updated_at: new Date().toISOString()
      }).eq("id", job.id);
      return json({ ok: true, job_id: job.id, status: "processing", provider: MODEL });
    }

    if (!job.provider_operation_id) return json({ error: "No Gemini operation is attached to this job" }, 409);
    const bytes = await finishVeo(job.provider_operation_id);
    if (!bytes) return json({ ok: true, job_id: job.id, status: "processing", message: "Gemini is still generating the video." });

    const url = await uploadR2(job.id, bytes);
    const now = new Date().toISOString();
    await admin.from("video_jobs").update({
      video_url: url,
      preview_url: url,
      generation_status: "published",
      generation_provider: "google-veo-3.1",
      generated_at: now,
      published_at: now,
      is_public: true,
      error_message: null,
      updated_at: now
    }).eq("id", job.id);
    await triggerYoutubeUpload(job.id);

    return json({ ok: true, job_id: job.id, status: "published", video_url: url });
  } catch (error) {
    return json({ error: error instanceof Error ? error.message : "Unexpected error" }, 500);
  }
});
