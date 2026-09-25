package com.example.data.remote

import com.example.data.model.VideoJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class VideoRepository(private val client: SupabaseClient) {
    private val http = OkHttpClient()

    private fun headers(builder: Request.Builder): Request.Builder =
        builder.addHeader("apikey", client.supabaseAnonKey)
            .addHeader("Authorization", "Bearer " + (client.authToken ?: client.supabaseAnonKey))
            .addHeader("Content-Type", "application/json")

    suspend fun listJobs(): List<VideoJob> = withContext(Dispatchers.IO) {
        val req = headers(Request.Builder()
            .url(client.supabaseUrl + "/rest/v1/video_jobs?select=*&order=created_at.desc&limit=50"))
            .get().build()
        http.newCall(req).execute().use { response ->
            if (!response.isSuccessful) error("Video jobs HTTP " + response.code)
            parseJobs(response.body?.string().orEmpty())
        }
    }

    suspend fun createJob(productId: String?, title: String, description: String, prompt: String, aspectRatio: String = "9:16"): VideoJob =
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("product_id", productId ?: JSONObject.NULL)
                put("created_by", client.currentUserId ?: JSONObject.NULL)
                put("title", title)
                put("description", description)
                put("generation_prompt", prompt)
                put("aspect_ratio", aspectRatio)
                put("resolution", "1080p")
                put("duration_seconds", 30)
                put("generation_status", "queued")
            }
            val req = headers(Request.Builder().url(client.supabaseUrl + "/rest/v1/video_jobs"))
                .addHeader("Prefer", "return=representation")
                .post(json.toString().toRequestBody("application/json".toMediaType())).build()
            http.newCall(req).execute().use { response ->
                if (!response.isSuccessful) error("Create video job HTTP " + response.code + ": " + response.body?.string())
                parseJobs(response.body?.string().orEmpty()).first()
            }
        }

    suspend fun generate(jobId: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("job_id", jobId).toString().toRequestBody("application/json".toMediaType())
        val req = headers(Request.Builder().url(client.supabaseUrl + "/functions/v1/video-generator"))
            .post(body).build()
        http.newCall(req).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Video generation HTTP " + response.code + ": " + text)
            JSONObject(text).optString("video_url")
        }
    }

    suspend fun publish(jobId: String, publish: Boolean): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("is_public", publish).put("published_at", if (publish) java.time.Instant.now().toString() else JSONObject.NULL)
            .toString().toRequestBody("application/json".toMediaType())
        val req = headers(Request.Builder().url(client.supabaseUrl + "/rest/v1/video_jobs?id=eq." + jobId))
            .patch(body).build()
        http.newCall(req).execute().use { it.isSuccessful }
    }

    private fun parseJobs(raw: String): List<VideoJob> {
        val arr = JSONArray(if (raw.isBlank()) "[]" else raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(VideoJob(
                    id = o.getString("id"), productId = o.optString("product_id").takeIf { it.isNotBlank() },
                    title = o.optString("title"), description = o.optString("description").takeIf { it.isNotBlank() },
                    videoUrl = o.optString("video_url").takeIf { it.isNotBlank() },
                    thumbnailUrl = o.optString("thumbnail_url").takeIf { it.isNotBlank() },
                    previewUrl = o.optString("preview_url").takeIf { it.isNotBlank() },
                    aspectRatio = o.optString("aspect_ratio", "9:16"), resolution = o.optString("resolution", "1080p"),
                    durationSeconds = o.optInt("duration_seconds", 30), generationStatus = o.optString("generation_status", "queued"),
                    generationProvider = o.optString("generation_provider").takeIf { it.isNotBlank() },
                    generationPrompt = o.optString("generation_prompt").takeIf { it.isNotBlank() },
                    scheduledAt = o.optString("scheduled_at").takeIf { it.isNotBlank() },
                    generatedAt = o.optString("generated_at").takeIf { it.isNotBlank() },
                    publishedAt = o.optString("published_at").takeIf { it.isNotBlank() },
                    downloadCount = o.optInt("download_count"), viewCount = o.optInt("view_count"),
                    isPublic = o.optBoolean("is_public"), errorMessage = o.optString("error_message").takeIf { it.isNotBlank() }
                ))
            }
        }
    }
}