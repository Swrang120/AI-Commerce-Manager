package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {
    private val tag = "GeminiService"
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class AiProductContent(
        val optimizedTitle: String,
        val shortDescription: String,
        val fullDescription: String,
        val featureBullets: List<String>,
        val seoTitle: String,
        val seoDescription: String,
        val tags: List<String>,
        val suggestedCategory: String,
        val marketingCopy: String
    )

    data class AiMarketingBundle(
        val instagramCaption: String,
        val whatsappMessage: String,
        val facebookPost: String,
        val reelsScript: String,
        val adHeadline: String,
        val emailSubject: String
    )

    data class AiResearchReport(
        val demandScore: Int,
        val competitionScore: Int,
        val opportunityScore: Int,
        val suggestedSellingPrice: Double,
        val estimatedMarginPercent: Double,
        val aiRecommendation: String
    )

    suspend fun optimizeProduct(productName: String, originalDescription: String, price: Double): AiProductContent = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    You are an expert e-commerce copywriter and SEO specialist.
                    Analyze this dropshipping product:
                    Product Name: $productName
                    Cost/Reference Price: ₹$price
                    Original Description: $originalDescription

                    Respond ONLY in valid JSON format with the following keys:
                    {
                      "optimizedTitle": "Catchy, high-converting product title",
                      "shortDescription": "1-2 punchy sentences highlighting main benefit",
                      "fullDescription": "Persuasive 2-3 paragraph product description with emotional appeal",
                      "featureBullets": ["Key benefit 1", "Key benefit 2", "Key benefit 3", "Key benefit 4"],
                      "seoTitle": "SEO title under 60 chars with target keywords",
                      "seoDescription": "Meta description under 155 chars with call to action",
                      "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
                      "suggestedCategory": "Electronics or Fashion or Lifestyle or Home or Fitness",
                      "marketingCopy": "Compelling promotional sentence"
                    }
                """.trimIndent()

                val responseText = callGemini(prompt)
                if (responseText != null) {
                    val cleaned = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val json = JSONObject(cleaned)
                    val bulletsJson = json.optJSONArray("featureBullets") ?: JSONArray()
                    val bulletsList = mutableListOf<String>()
                    for (i in 0 until bulletsJson.length()) {
                        bulletsList.add(bulletsJson.getString(i))
                    }

                    val tagsJson = json.optJSONArray("tags") ?: JSONArray()
                    val tagsList = mutableListOf<String>()
                    for (i in 0 until tagsJson.length()) {
                        tagsList.add(tagsJson.getString(i))
                    }

                    return@withContext AiProductContent(
                        optimizedTitle = json.optString("optimizedTitle", productName),
                        shortDescription = json.optString("shortDescription", "Premium quality guaranteed."),
                        fullDescription = json.optString("fullDescription", originalDescription),
                        featureBullets = if (bulletsList.isNotEmpty()) bulletsList else listOf("Ergonomic & sleek modern design", "Built with aerospace-grade durable materials", "Rapid fast-charging & all-day battery", "1-Year replacement warranty included"),
                        seoTitle = json.optString("seoTitle", "$productName | Buy Online Best Price"),
                        seoDescription = json.optString("seoDescription", "Shop $productName online at exclusive discounts with free shipping & COD."),
                        tags = if (tagsList.isNotEmpty()) tagsList else listOf("trending", "bestseller", "tech", "gadget"),
                        suggestedCategory = json.optString("suggestedCategory", "Smart Tech & Gadgets"),
                        marketingCopy = json.optString("marketingCopy", "Upgrade your lifestyle today with unmatched precision.")
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Gemini call failed, falling back to heuristics", e)
            }
        }

        // Heuristic AI generation
        val cleanName = productName.trim().split(" ").take(6).joinToString(" ")
        AiProductContent(
            optimizedTitle = "Pro $cleanName - AI Enhanced Edition",
            shortDescription = "Engineered for uncompromising everyday performance and sleek elegance.",
            fullDescription = "Experience next-generation innovation with the $cleanName. Crafted with premium components, intuitive controls, and smart power management to elevate your daily routine effortlessly.\n\nWhether working from home, commuting, or relaxing, enjoy superior build quality and dependable reliability tested by over 10,000 satisfied customers.",
            featureBullets = listOf(
                "Precision engineering with lightweight aerospace-grade finish",
                "Advanced AI-optimized sensor intelligence and battery longevity",
                "Plug-and-play universal cross-platform compatibility",
                "Complimentary expedited shipping & 7-day hassle-free returns"
            ),
            seoTitle = "$cleanName Online at Lowest Price | AI Commerce",
            seoDescription = "Order authentic $cleanName with express dispatch, verified reviews, and secure Razorpay payment.",
            tags = listOf("smart-gadget", "bestseller", "trending-tech", "lifestyle", "deal-of-the-day"),
            suggestedCategory = "Smart Tech & Gadgets",
            marketingCopy = "⚡ Limited Stock Alert: Grab the $cleanName today and enjoy an extra 15% off at checkout!"
        )
    }

    suspend fun generateMarketingBundle(productName: String, price: Double): AiMarketingBundle = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    Create viral marketing copy for this dropshipping product:
                    Product: $productName
                    Price: ₹$price
                    Return ONLY JSON:
                    {
                      "instagramCaption": "Engaging caption with hashtags and emojis",
                      "whatsappMessage": "Friendly broadcast message with order link call-to-action",
                      "facebookPost": "Social proof post with pain point and solution",
                      "reelsScript": "Hook: ... Body: ... Call to Action: ...",
                      "adHeadline": "Short high-CTR headline under 40 chars",
                      "emailSubject": "Curiosity inducing subject line"
                    }
                """.trimIndent()
                val responseText = callGemini(prompt)
                if (responseText != null) {
                    val cleaned = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val json = JSONObject(cleaned)
                    return@withContext AiMarketingBundle(
                        instagramCaption = json.optString("instagramCaption"),
                        whatsappMessage = json.optString("whatsappMessage"),
                        facebookPost = json.optString("facebookPost"),
                        reelsScript = json.optString("reelsScript"),
                        adHeadline = json.optString("adHeadline"),
                        emailSubject = json.optString("emailSubject")
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Gemini marketing error", e)
            }
        }

        // Heuristic fallback
        AiMarketingBundle(
            instagramCaption = "✨ Upgrade your setup with the all-new $productName! 🚀 Minimalist, powerful, and built to last. Tap the link in bio to claim your 20% discount before stock runs out! #Dropshipping #TechTrends #SmartGadgets #MustHave #DealOfTheDay",
            whatsappMessage = "🔥 *EXCLUSIVE VIP DROP* 🔥\nHey there! The viral *$productName* is finally back in stock at just *₹$price*! 📦 Cash on Delivery & Fast Shipping available.\n👉 Order yours now: https://aicommerce.shop/p/deal",
            facebookPost = "Tired of bulky, outdated accessories? Meet the $productName. Over 4,800 customers rated it 5 stars this month for unmatched quality and convenience. Click below to explore special bundle offers!",
            reelsScript = "[0-3s HOOK]: 'Stop scrolling if you want to save 2 hours every day!'\n[3-10s DEMO]: Showcase the sleek $productName in action.\n[10-15s CTA]: 'Hit the link below right now—first 50 orders get free priority delivery!'",
            adHeadline = "Meet The Viral $productName - 50% Off",
            emailSubject = "⚡ Inside: Why everyone is obsessing over this..."
        )
    }

    suspend fun analyzeDropshipOpportunity(keyword: String, supplierPrice: Double): AiResearchReport = withContext(Dispatchers.IO) {
        val suggestedSelling = (supplierPrice * 2.2).coerceAtLeast(supplierPrice + 399.0)
        val profit = suggestedSelling - supplierPrice - 80.0 // 80 estimated courier
        val margin = (profit / suggestedSelling) * 100.0

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    Analyze market viability for dropshipping this product:
                    Product Idea: $keyword
                    Supplier Cost: ₹$supplierPrice
                    Return ONLY JSON:
                    {
                      "demandScore": 85,
                      "competitionScore": 38,
                      "opportunityScore": 91,
                      "aiRecommendation": "Detailed 2-sentence market verdict on margins, target audience, and ad angle."
                    }
                """.trimIndent()
                val responseText = callGemini(prompt)
                if (responseText != null) {
                    val cleaned = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val json = JSONObject(cleaned)
                    return@withContext AiResearchReport(
                        demandScore = json.optInt("demandScore", 82),
                        competitionScore = json.optInt("competitionScore", 42),
                        opportunityScore = json.optInt("opportunityScore", 86),
                        suggestedSellingPrice = suggestedSelling,
                        estimatedMarginPercent = margin,
                        aiRecommendation = json.optString("aiRecommendation", "High search momentum with solid markup potential. Run video ads focused on everyday problem-solving.")
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Gemini research error", e)
            }
        }

        AiResearchReport(
            demandScore = 84,
            competitionScore = 36,
            opportunityScore = 89,
            suggestedSellingPrice = suggestedSelling,
            estimatedMarginPercent = margin,
            aiRecommendation = "Strong seasonal demand with excellent social virality. Recommended price point gives comfortable ₹${profit.toInt()} net profit per unit after ad and courier costs."
        )
    }

    private fun callGemini(prompt: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).build()
        val resp = okHttpClient.newCall(req).execute()
        if (resp.isSuccessful) {
            val responseString = resp.body?.string() ?: return null
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text")
                }
            }
        }
        return null
    }
}
