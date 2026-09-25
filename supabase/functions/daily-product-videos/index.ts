import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const providerUrl = Deno.env.get("VIDEO_PROVIDER_URL");
const providerApiKey = Deno.env.get("VIDEO_PROVIDER_API_KEY");
const admin = createClient(supabaseUrl, serviceRoleKey, { auth: { persistSession: false, autoRefreshToken: false } });

const BRAND = "E-commerce Shoping";
const BUCKET = "product-videos";

async function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json" } });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "POST required" }, 405);
  try {
    if (!providerUrl || !providerApiKey) return json({ error: "VIDEO_PROVIDER_URL / VIDEO_PROVIDER_API_KEY not configured" }, 503);

    const { data: settings, error: settingsError } = await admin
      .from("product_autopilot_settings")
      .select("product_id")
      .eq("enabled", true)
      .eq("video_generation", true)
      .limit(20);
    if (settingsError) throw settingsError;

    const productIds = (settings ?? []).map((x: any) => x.product_id).filter(Boolean);
    if (!productIds.length) return json({ ok: true, created: 0, message: "No products have video autopilot enabled." });

    const since = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
    const { data: recent } = await admin
      .from("video_jobs")
      .select("product_id")
      .in("product_id", productIds)
      .gte("created_at", since);
    const recentIds = new Set((recent ?? []).map((x: any) => x.product_id));
    const candidates = productIds.filter((id: string) => !recentIds.has(id)).slice(0, 5);

    const created: string[] = [];
    for (const productId of candidates) {
      const { data: product, error: productError } = await admin
        .from("products")
        .select("id,name,description,short_description,price,images")
        .eq("id", productId)
        .single();
      if (productError || !product) continue;

      const title = product.name + " — " + BRAND;
      const prompt = [
        "Create a premium product-promotion video using only the supplied real product information.",
        "Do not invent specifications, reviews, prices, discounts, certifications, or performance claims.",
        "Use a clean modern e-commerce presentation, product-focused visuals, readable call-to-action.",
        "Visible persistent brand watermark required on the video: " + BRAND + ".",
        "Watermark placement: bottom-right, legible but not covering the product.",
        "Format: 9:16 vertical, 1080p, about 30 seconds.",
        "Product name: " + product.name,
        "Description: " + (product.description ?? product.short_description ?? ""),
        "Current price: " + String(product.price ?? ""),
        "Product images: " + JSON.stringify(product.images ?? [])
      ].join("\n");

      const { data: job, error: insertError } = await admin.from("video_jobs").insert({
        product_id: product.id,
        title,
        description: "Daily AI product promotion for " + BRAND,
        aspect_ratio: "9:16",
        resolution: "1080p",
        duration_seconds: 30,
        generation_status: "processing",
        generation_provider: "configured-video-provider",
        generation_prompt: prompt
      }).select("id").single();
      if (insertError || !job) continue;

      try {
        const providerResponse = await fetch(providerUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json", "Authorization": "Bearer " + providerApiKey },
          body: JSON.stringify({
            job_id: job.id,
            product: { id: product.id, name: product.name, description: product.description ?? product.short_description, price: product.price, images: product.images ?? [] },
            prompt,
            aspect_ratio: "9:16",
            resolution: "1080p",
            duration_seconds: 30,
            watermark: { text: BRAND, position: "bottom-right", required: true }
          })
        });
        const bodyText = await providerResponse.text();
        if (!providerResponse.ok) throw new Error("Video provider HTTP " + providerResponse.status + ": " + bodyText.slice(0, 400));
        const result = JSON.parse(bodyText);
        if (!result.video_url) throw new Error("Video provider returned no video_url");

        const videoResponse = await fetch(result.video_url);
        if (!videoResponse.ok) throw new Error("Could not download generated video");
        const videoBytes = new Uint8Array(await videoResponse.arrayBuffer());
        const storagePath = product.id + "/" + job.id + ".mp4";
        const upload = await admin.storage.from(BUCKET).upload(storagePath, videoBytes, {
          contentType: "video/mp4",
          upsert: true,
          cacheControl: "31536000"
        });
        if (upload.error) throw upload.error;

        const publicUrl = admin.storage.from(BUCKET).getPublicUrl(storagePath).data.publicUrl;
        const now = new Date().toISOString();
        await admin.from("video_jobs").update({
          video_url: publicUrl,
          preview_url: publicUrl,
          thumbnail_url: result.thumbnail_url ?? null,
          generation_status: "published",
          generated_at: now,
          published_at: now,
          is_public: true,
          error_message: null,
          updated_at: now
        }).eq("id", job.id);
        created.push(job.id);
      } catch (error) {
        await admin.from("video_jobs").update({
          generation_status: "failed",
          error_message: error instanceof Error ? error.message : "Video generation failed",
          updated_at: new Date().toISOString()
        }).eq("id", job.id);
      }
    }

    return json({ ok: true, created: created.length, job_ids: created, brand: BRAND });
  } catch (error) {
    return json({ error: error instanceof Error ? error.message : "Unexpected error" }, 500);
  }
});
