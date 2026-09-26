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

const BRAND = "E-commerce Shoping";
const MODEL = "veo-3.1-generate-preview";
const GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta";

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json" } });
}

function b64(bytes: Uint8Array) {
  let out = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    out += String.fromCharCode(...bytes.subarray(i, Math.min(i + chunk, bytes.length)));
  }
  return btoa(out);
}

function r2Client() {
  if (!r2AccountId || !r2AccessKeyId || !r2SecretAccessKey || !r2PublicBaseUrl) {
    throw new Error("R2 storage is not configured");
  }
  return new S3Client({
    region: "auto",
    endpoint: `https://${r2AccountId}.r2.cloudflarestorage.com`,
    credentials: { accessKeyId: r2AccessKeyId, secretAccessKey: r2SecretAccessKey }
  });
}

async function productImage(product: any) {
  const images = Array.isArray(product.images) ? product.images : [];
  const url = images.find((x: any) => typeof x === "string" && /^https?:\/\//i.test(x));
  if (!url) return null;
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const mimeType = (response.headers.get("content-type") || "image/jpeg").split(";")[0];
    if (!mimeType.startsWith("image/")) return null;
    const bytes = new Uint8Array(await response.arrayBuffer());
    if (!bytes.length || bytes.length > 5_000_000) return null;
    return { mimeType, data: b64(bytes) };
  } catch {
    return null;
  }
}

async function startVeo(prompt: string, image: { mimeType: string; data: string } | null) {
  if (!geminiKey) throw new Error("GEMINI_API_KEY is not configured");
  const instance: any = { prompt };
  if (image) instance.image = { inlineData: { mimeType: image.mimeType, data: image.data } };

  const response = await fetch(`${GEMINI_BASE}/models/${MODEL}:predictLongRunning`, {
    method: "POST",
    headers: { "x-goog-api-key": geminiKey, "Content-Type": "application/json" },
    body: JSON.stringify({
      instances: [instance],
      parameters: { aspectRatio: "9:16", resolution: "1080p", numberOfVideos: 1 }
    })
  });
  const text = await response.text();
  if (!response.ok) throw new Error("Gemini HTTP " + response.status + ": " + text.slice(0, 500));
  const data = JSON.parse(text);
  if (!data.name) throw new Error("Gemini did not return an operation name");
  return data.name as string;
}

async function finishVeo(operationName: string) {
  if (!geminiKey) throw new Error("GEMINI_API_KEY is not configured");
  const response = await fetch(`${GEMINI_BASE}/${operationName}`, {
    headers: { "x-goog-api-key": geminiKey }
  });
  const text = await response.text();
  if (!response.ok) throw new Error("Gemini operation HTTP " + response.status + ": " + text.slice(0, 500));
  const data = JSON.parse(text);
  if (!data.done) return { done: false as const };
  if (data.error) throw new Error(data.error.message || "Gemini video generation failed");
  const uri = data.response?.generateVideoResponse?.generatedSamples?.[0]?.video?.uri;
  if (!uri) throw new Error("Gemini completed without a video URI");
  const video = await fetch(uri, { headers: { "x-goog-api-key": geminiKey } });
  if (!video.ok) throw new Error("Could not download Gemini video");
  return { done: true as const, bytes: new Uint8Array(await video.arrayBuffer()) };
}

async function uploadR2(jobId: string, bytes: Uint8Array) {
  const key = `videos/${jobId}.mp4`;
  await r2Client().send(new PutObjectCommand({
    Bucket: r2Bucket,
    Key: key,
    Body: bytes,
    ContentType: "video/mp4",
    CacheControl: "public, max-age=31536000"
  }));
  return `${r2PublicBaseUrl}/${key}`;
}
async function triggerYoutubeUpload(jobId: string) {
  try {
    await fetch(`${supabaseUrl}/functions/v1/youtube-upload`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": "Bearer " + serviceRoleKey },
      body: JSON.stringify({ job_id: jobId })
    });
  } catch {
    // Keep the R2 video published even if YouTube is not configured or temporarily unavailable.
  }
}

async function pollAndPublish(job: any) {
  if (!job.provider_operation_id) return false;
  const result = await finishVeo(job.provider_operation_id);
  if (!result.done) return false;
  const url = await uploadR2(job.id, result.bytes);
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
  return true;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "POST required" }, 405);
  try {
    const auth = req.headers.get("Authorization");
    const token = auth?.replace(/^Bearer\s+/i, "");
    if (!serviceRoleKey || token !== serviceRoleKey) return json({ error: "Service authorization required" }, 401);

    if (!geminiKey || !r2AccountId || !r2AccessKeyId || !r2SecretAccessKey || !r2PublicBaseUrl) {
      return json({ error: "Gemini/R2 storage secrets are not configured" }, 503);
    }

    // Finish an already-processing generation before starting another job.
    // Finish one previously started generation first.
    const { data: pending } = await admin.from("video_jobs")
      .select("*")
      .eq("generation_status", "processing")
      .not("provider_operation_id", "is", null)
      .order("created_at", { ascending: true })
      .limit(1);
    if (pending?.[0]) {
      try {
        const finished = await pollAndPublish(pending[0]);
        if (finished) return json({ ok: true, completed_job_id: pending[0].id, created: 0 });
      } catch (error) {
        await admin.from("video_jobs").update({
          generation_status: "failed",
          error_message: error instanceof Error ? error.message : "Video generation failed",
          updated_at: new Date().toISOString()
        }).eq("id", pending[0].id);
      }
    }



    // Global campaign queue: process one queued country/language variant per run.
    const { data: globalQueue } = await admin.from("video_jobs")
      .select("*,products(id,name,description,short_description,selling_price,images)")
      .eq("generation_status","queued")
      .not("country_code","is",null)
      .not("language_code","is",null)
      .order("created_at",{ascending:true})
      .limit(1);
    if(globalQueue?.[0]){
      const job=globalQueue[0];
      const product=job.products;
      if(product){
        const prompt=[
          "Create a premium e-commerce product promotion video.",
          "Localize narration, on-screen wording and natural audio for language code: "+String(job.language_code||"en")+".",
          "Target market country code: "+String(job.country_code||"")+" and locale: "+String(job.locale||"")+".",
          "Use natural local phrasing, not literal machine translation.",
          "Use only verified product facts. Never invent specifications, reviews, discounts, certifications, prices, or performance claims.",
          "Show the product clearly with smooth commercial lighting and camera motion.",
          "Vertical 9:16 social-commerce video with appropriate local audio.",
          "Persistent watermark: "+BRAND+", bottom-right.",
          "Product name: "+product.name,
          "Current price: "+String(product.selling_price??""),
          "Description: "+String(product.description??product.short_description??"")
        ].join("\n");
        const operation=await startVeo(prompt,await productImage(product));
        await admin.from("video_jobs").update({generation_status:"processing",generation_provider:"google-veo-3.1",provider_operation_id:operation,generation_prompt:prompt,duration_seconds:8,updated_at:new Date().toISOString()}).eq("id",job.id);
        return json({ok:true,global_job_id:job.id,status:"processing"});
      }
    }

    const { data: settings, error: settingsError } = await admin.from("product_autopilot_settings")
      .select("product_id").eq("enabled", true).eq("video_generation", true).limit(20);
    if (settingsError) throw settingsError;
    const productIds = (settings ?? []).map((x: any) => x.product_id).filter(Boolean);
    if (!productIds.length) return json({ ok: true, created: 0, message: "No video-autopilot products." });

    const since = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
    const { data: recent } = await admin.from("video_jobs").select("product_id")
      .in("product_id", productIds).gte("created_at", since);
    const recentIds = new Set((recent ?? []).map((x: any) => x.product_id));
    const candidate = productIds.find((id: string) => !recentIds.has(id));
    if (!candidate) return json({ ok: true, created: 0, message: "Daily video already generated for eligible products." });

    const { data: product, error: productError } = await admin.from("products")
      .select("id,name,description,short_description,price,images").eq("id", candidate).single();
    if (productError || !product) return json({ ok: true, created: 0, message: "Product not found." });

    const prompt = [
      "Create a premium e-commerce product promotion video.",
      "Use only the supplied product facts. Never invent specifications, reviews, discounts, certifications, prices, or performance claims.",
      "Use the supplied product image as the primary product reference when available.",
      "Show the product clearly with clean commercial lighting and smooth camera motion.",
      "Create a vertical 9:16 social-commerce video with native appropriate audio.",
      "Include a subtle persistent visible watermark: " + BRAND + ", bottom-right.",
      "Product name: " + product.name,
      "Description: " + (product.description ?? product.short_description ?? ""),
      "Current price: " + String(product.selling_price ?? "")
    ].join("\n");

    const { data: job, error: insertError } = await admin.from("video_jobs").insert({
      product_id: product.id,
      title: product.name + " — " + BRAND,
      description: "Automated AI product video",
      aspect_ratio: "9:16",
      resolution: "1080p",
      duration_seconds: 8,
      generation_status: "processing",
      generation_provider: "google-veo-3.1",
      generation_prompt: prompt
    }).select("*").single();
    if (insertError || !job) throw insertError || new Error("Could not create video job");

    const operation = await startVeo(prompt, await productImage(product));
    await admin.from("video_jobs").update({
      provider_operation_id: operation,
      updated_at: new Date().toISOString()
    }).eq("id", job.id);

    return json({ ok: true, created: 1, job_id: job.id, status: "processing", provider: MODEL });
  } catch (error) {
    return json({ error: error instanceof Error ? error.message : "Unexpected error" }, 500);
  }
});
