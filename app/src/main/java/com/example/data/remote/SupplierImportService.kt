package com.example.data.remote

import com.example.data.model.Product
import com.example.data.model.ProductSource
import com.example.data.model.ProductVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

data class NormalizedProductImport(
    val name: String,
    val description: String = "",
    val supplierPrice: Double = 0.0,
    val currency: String = "INR",
    val images: List<String> = emptyList(),
    val sku: String = "",
    val externalId: String? = null,
    val availability: String = "in_stock",
    val sourceName: String,
    val sourceUrl: String,
    val category: String? = null,
    val specifications: Map<String, String> = emptyMap(),
    val suggestedShipping: Double = 79.0,
    val suggestedSellingPrice: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val isAutoExtracted: Boolean = false
)

sealed class ImportResult {
    data class Success(val product: NormalizedProductImport) : ImportResult()
    data class Unavailable(
        val reason: String,
        val manualEntryTemplate: NormalizedProductImport
    ) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

interface ProductSourceProvider {
    fun canHandle(url: String): Boolean
    fun providerName(): String
    suspend fun importProduct(url: String): ImportResult
}

class AmazonProvider : ProductSourceProvider {
    override fun canHandle(url: String): Boolean = url.contains("amazon.", ignoreCase = true)
    override fun providerName(): String = "Amazon"

    override suspend fun importProduct(url: String): ImportResult {
        val asin = extractAsin(url)
        val template = NormalizedProductImport(
            name = "",
            description = "",
            supplierPrice = 0.0,
            images = emptyList(),
            sku = "SKU-AMZ-${asin ?: UUID.randomUUID().toString().take(6).uppercase()}",
            externalId = asin,
            sourceName = "Amazon",
            sourceUrl = url,
            isAutoExtracted = false
        )
        // Per terms of service and policy, we do not scrape without authorized Selling Partner API
        return ImportResult.Unavailable(
            reason = "Automatic import is unavailable for this source. Amazon Selling Partner API credentials are required. Use an approved API/feed or enter the product information manually.",
            manualEntryTemplate = template
        )
    }

    private fun extractAsin(url: String): String? {
        return try {
            val path = URI(url.trim()).path ?: ""
            val dpIndex = path.indexOf("/dp/")
            if (dpIndex != -1) path.substring(dpIndex + 4).split("/").firstOrNull()?.take(10)
            else null
        } catch (e: Exception) {
            null
        }
    }
}

class FlipkartProvider : ProductSourceProvider {
    override fun canHandle(url: String): Boolean = url.contains("flipkart.", ignoreCase = true)
    override fun providerName(): String = "Flipkart"

    override suspend fun importProduct(url: String): ImportResult {
        val pid = extractPid(url)
        val template = NormalizedProductImport(
            name = "",
            description = "",
            supplierPrice = 0.0,
            images = emptyList(),
            sku = "SKU-FLP-${pid ?: UUID.randomUUID().toString().take(6).uppercase()}",
            externalId = pid,
            sourceName = "Flipkart",
            sourceUrl = url,
            isAutoExtracted = false
        )
        return ImportResult.Unavailable(
            reason = "Automatic import is unavailable for this source. Flipkart Marketplace Affiliate/Seller API integration is required. Use an approved API/feed or enter the product information manually.",
            manualEntryTemplate = template
        )
    }

    private fun extractPid(url: String): String? {
        return try {
            val uri = URI(url.trim())
            uri.query?.split("&")?.find { it.startsWith("pid=") }?.removePrefix("pid=")
        } catch (e: Exception) {
            null
        }
    }
}

class MeeshoProvider : ProductSourceProvider {
    override fun canHandle(url: String): Boolean = url.contains("meesho.", ignoreCase = true)
    override fun providerName(): String = "Meesho"

    override suspend fun importProduct(url: String): ImportResult {
        val template = NormalizedProductImport(
            name = "",
            description = "",
            supplierPrice = 0.0,
            images = emptyList(),
            sku = "SKU-MSH-${UUID.randomUUID().toString().take(6).uppercase()}",
            sourceName = "Meesho",
            sourceUrl = url,
            isAutoExtracted = false
        )
        return ImportResult.Unavailable(
            reason = "Automatic import is unavailable for this source. Meesho Seller Feed authentication is required. Use an approved API/feed or enter the product information manually.",
            manualEntryTemplate = template
        )
    }
}

class ShopifyProvider(private val httpClient: OkHttpClient) : ProductSourceProvider {
    override fun canHandle(url: String): Boolean =
        url.contains("myshopify.com", ignoreCase = true) || url.contains("/products/")
    override fun providerName(): String = "Shopify"

    override suspend fun importProduct(url: String): ImportResult = withContext(Dispatchers.IO) {
        val jsonUrl = try {
            val uri = URI(url.trim())
            val path = uri.path?.trimEnd('/') ?: ""
            if (path.contains("/products/")) {
                "${uri.scheme}://${uri.host}$path.json"
            } else null
        } catch (e: Exception) {
            null
        }

        if (jsonUrl != null) {
            try {
                val req = Request.Builder()
                    .url(jsonUrl)
                    .header("User-Agent", "NexusAICommerce/1.0")
                    .get()
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyString = resp.body?.string() ?: ""
                    val root = JSONObject(bodyString)
                    val prodObj = root.optJSONObject("product")
                    if (prodObj != null) {
                        val title = prodObj.optString("title")
                        val bodyHtml = prodObj.optString("body_html", "")
                            .replace(Regex("<[^>]*>"), "")
                            .trim()
                        val imagesArr = prodObj.optJSONArray("images")
                        val imagesList = mutableListOf<String>()
                        if (imagesArr != null) {
                            for (i in 0 until imagesArr.length()) {
                                val img = imagesArr.getJSONObject(i).optString("src")
                                if (img.isNotBlank()) imagesList.add(img)
                            }
                        }

                        val variantsArr = prodObj.optJSONArray("variants")
                        var firstPrice = 0.0
                        var firstSku = ""
                        if (variantsArr != null && variantsArr.length() > 0) {
                            val v = variantsArr.getJSONObject(0)
                            firstPrice = v.optDouble("price", 0.0)
                            firstSku = v.optString("sku", "")
                        }

                        val suggestedPrice = (firstPrice * 1.45).let { Math.round(it / 10.0) * 10 - 1.0 }.coerceAtLeast(firstPrice)
                        val margin = if (suggestedPrice > 0) ((suggestedPrice - firstPrice - 79.0) / suggestedPrice) * 100.0 else 0.0

                        return@withContext ImportResult.Success(
                            NormalizedProductImport(
                                name = title,
                                description = bodyHtml,
                                supplierPrice = firstPrice,
                                images = imagesList,
                                sku = if (firstSku.isNotBlank()) firstSku else "SKU-SHP-${UUID.randomUUID().toString().take(6).uppercase()}",
                                externalId = prodObj.optString("id"),
                                sourceName = "Shopify",
                                sourceUrl = url,
                                category = prodObj.optString("product_type"),
                                suggestedShipping = 79.0,
                                suggestedSellingPrice = suggestedPrice,
                                profitMarginPercent = margin,
                                isAutoExtracted = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // If official JSON endpoint is closed or throws, fall through to Unavailable
            }
        }

        val template = NormalizedProductImport(
            name = "",
            description = "",
            supplierPrice = 0.0,
            images = emptyList(),
            sku = "SKU-SHP-${UUID.randomUUID().toString().take(6).uppercase()}",
            sourceName = "Shopify",
            sourceUrl = url,
            isAutoExtracted = false
        )
        return@withContext ImportResult.Unavailable(
            reason = "Automatic import is unavailable for this store. Public API access is disabled by the merchant. Use an approved API/feed or enter the product information manually.",
            manualEntryTemplate = template
        )
    }
}

class GenericProvider : ProductSourceProvider {
    override fun canHandle(url: String): Boolean = true
    override fun providerName(): String = "Generic Provider"

    override suspend fun importProduct(url: String): ImportResult {
        val template = NormalizedProductImport(
            name = "",
            description = "",
            supplierPrice = 0.0,
            images = emptyList(),
            sku = "SKU-GEN-${UUID.randomUUID().toString().take(6).uppercase()}",
            sourceName = "Generic Provider",
            sourceUrl = url,
            isAutoExtracted = false
        )
        return ImportResult.Unavailable(
            reason = "Automatic import is unavailable for this source. Use an approved API/feed or enter the product information manually.",
            manualEntryTemplate = template
        )
    }
}

class SupplierImportService {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val providers: List<ProductSourceProvider> = listOf(
        AmazonProvider(),
        FlipkartProvider(),
        MeeshoProvider(),
        ShopifyProvider(httpClient),
        GenericProvider()
    )

    fun detectProvider(url: String): ProductSourceProvider {
        val trimmed = url.trim()
        return providers.firstOrNull { it.canHandle(trimmed) } ?: GenericProvider()
    }

    suspend fun parseProductUrl(url: String, selectedSource: String = "Auto-Detect"): ImportResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return ImportResult.Error("Please enter a valid product or feed URL.")
        }

        val provider = if (selectedSource != "Auto-Detect") {
            providers.find { it.providerName().equals(selectedSource, ignoreCase = true) }
                ?: detectProvider(trimmed)
        } else {
            detectProvider(trimmed)
        }

        return provider.importProduct(trimmed)
    }

    fun toProduct(
        import: NormalizedProductImport,
        customSellingPrice: Double? = null,
        customMargin: Double? = null,
        categoryId: String? = null,
        supplierId: String? = null
    ): Product {
        val selling = customSellingPrice ?: import.suggestedSellingPrice
        val profit = (selling - import.supplierPrice - import.suggestedShipping).coerceAtLeast(0.0)
        val margin = if (selling > 0) (profit / selling) * 100.0 else (customMargin ?: 0.0)

        val cleanName = if (import.name.isNotBlank()) import.name else "Draft Imported Product"
        val slug = cleanName.lowercase().replace("[^a-z0-9]+".toRegex(), "-").trim('-') + "-" + (100..999).random()

        return Product(
            name = cleanName,
            slug = slug,
            sku = if (import.sku.isNotBlank()) import.sku else "SKU-IMP-${UUID.randomUUID().toString().take(6).uppercase()}",
            description = import.description,
            shortDescription = if (import.description.isNotBlank()) import.description.take(120) + "..." else null,
            categoryId = categoryId,
            supplierId = supplierId,
            costPrice = import.supplierPrice,
            shippingCost = import.suggestedShipping,
            desiredProfit = profit,
            sellingPrice = selling,
            compareAtPrice = if (selling > 0) selling * 1.35 else null,
            profitMargin = margin,
            stockQuantity = 0,
            status = "draft", // Default imported status: draft (NOT active!)
            images = import.images,
            tags = listOf(import.sourceName.lowercase(), "imported", "draft"),
            seoTitle = "$cleanName | Buy Online",
            seoDescription = "Order $cleanName at competitive price with fast fulfillment.",
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        )
    }
}
