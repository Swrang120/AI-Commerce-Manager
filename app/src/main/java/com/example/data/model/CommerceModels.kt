package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slug: String,
    val sku: String? = null,
    val description: String = "",
    @Json(name = "short_description") val shortDescription: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "supplier_id") val supplierId: String? = null,
    @Json(name = "cost_price") val costPrice: Double = 0.0,
    @Json(name = "shipping_cost") val shippingCost: Double = 0.0,
    @Json(name = "desired_profit") val desiredProfit: Double = 0.0,
    @Json(name = "selling_price") val sellingPrice: Double = 0.0,
    @Json(name = "compare_at_price") val compareAtPrice: Double? = null,
    @Json(name = "profit_margin") val profitMargin: Double = 0.0,
    @Json(name = "stock_quantity") val stockQuantity: Int = 0,
    val status: String = "draft", // active, draft, pending_review, out_of_stock, paused, archived
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    @Json(name = "seo_title") val seoTitle: String? = null,
    @Json(name = "seo_description") val seoDescription: String? = null,
    @Json(name = "category_name") val categoryName: String? = null,
    @Json(name = "supplier_name") val supplierName: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slug: String,
    val icon: String? = null,
    val description: String? = null,
    @Json(name = "product_count") val productCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class Supplier(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val website: String? = null,
    val type: String = "Marketplace", // Marketplace, Manufacturer, Wholesaler, Affiliate, Shopify, Manual, Other
    val contact: String? = null,
    @Json(name = "api_status") val apiStatus: String = "Not Configured",
    val status: String = "active", // active, inactive
    val notes: String? = null,
    @Json(name = "products_count") val productsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ProductVariant(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "product_id") val productId: String,
    val title: String,
    val sku: String? = null,
    val price: Double,
    @Json(name = "cost_price") val costPrice: Double? = null,
    @Json(name = "stock_quantity") val stockQuantity: Int = 0
)

@JsonClass(generateAdapter = true)
data class ProductSource(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "product_id") val productId: String,
    @Json(name = "source_name") val sourceName: String, // Amazon, Flipkart, Meesho, Shopify, Generic Provider
    @Json(name = "source_url") val sourceUrl: String,
    @Json(name = "external_product_id") val externalProductId: String? = null,
    @Json(name = "supplier_price") val supplierPrice: Double = 0.0,
    val currency: String = "INR",
    val availability: String = "in_stock",
    @Json(name = "last_scraped_at") val lastScrapedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Order(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "order_number") val orderNumber: String,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_email") val customerEmail: String,
    @Json(name = "customer_phone") val customerPhone: String? = null,
    @Json(name = "shipping_address") val shippingAddress: String,
    val subtotal: Double,
    @Json(name = "shipping_cost") val shippingCost: Double = 0.0,
    @Json(name = "discount_amount") val discountAmount: Double = 0.0,
    @Json(name = "tax_amount") val taxAmount: Double = 0.0,
    @Json(name = "total_amount") val totalAmount: Double,
    val status: String = "pending", // pending, confirmed, paid, processing, supplier_pending, supplier_ordered, shipped, delivered, cancelled, refunded, failed
    @Json(name = "payment_status") val paymentStatus: String = "pending", // pending, authorized, captured, failed, refunded, partially_refunded
    @Json(name = "payment_method") val paymentMethod: String = "Razorpay",
    @Json(name = "razorpay_order_id") val razorpayOrderId: String? = null,
    @Json(name = "razorpay_payment_id") val razorpayPaymentId: String? = null,
    @Json(name = "supplier_status") val supplierStatus: String = "pending",
    val items: List<OrderItem> = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "order_id") val orderId: String = "",
    @Json(name = "product_id") val productId: String? = null,
    @Json(name = "variant_id") val variantId: String? = null,
    @Json(name = "product_name") val productName: String,
    val sku: String? = null,
    @Json(name = "unit_price") val unitPrice: Double,
    @Json(name = "cost_price") val costPrice: Double = 0.0,
    val quantity: Int,
    @Json(name = "total_price") val totalPrice: Double
)

@JsonClass(generateAdapter = true)
data class Shipment(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "order_id") val orderId: String,
    @Json(name = "courier_name") val courierName: String,
    @Json(name = "tracking_number") val trackingNumber: String,
    @Json(name = "tracking_url") val trackingUrl: String? = null,
    val status: String = "pending", // pending, label_created, shipped, in_transit, out_for_delivery, delivered, returned, cancelled
    @Json(name = "shipped_at") val shippedAt: String? = null,
    @Json(name = "estimated_delivery") val estimatedDelivery: String? = null,
    @Json(name = "delivered_at") val deliveredAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentRecord(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "order_id") val orderId: String,
    @Json(name = "razorpay_order_id") val razorpayOrderId: String? = null,
    @Json(name = "razorpay_payment_id") val razorpayPaymentId: String? = null,
    @Json(name = "razorpay_signature") val razorpaySignature: String? = null,
    val amount: Double,
    val currency: String = "INR",
    val status: String = "pending", // pending, authorized, captured, failed, refunded, partially_refunded
    val method: String = "Razorpay (UPI / Card)",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Customer(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    @Json(name = "total_orders") val totalOrders: Int = 0,
    @Json(name = "total_spent") val totalSpent: Double = 0.0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CustomerAddress(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "address_line1") val addressLine1: String,
    @Json(name = "address_line2") val addressLine2: String? = null,
    val city: String,
    val state: String,
    @Json(name = "postal_code") val postalCode: String,
    val country: String = "India",
    @Json(name = "is_default") val isDefault: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Coupon(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    @Json(name = "discount_type") val discountType: String = "percentage", // percentage, fixed
    @Json(name = "discount_value") val discountValue: Double,
    @Json(name = "minimum_order_amount") val minimumOrderAmount: Double = 0.0,
    @Json(name = "maximum_discount") val maximumDiscount: Double? = null,
    @Json(name = "usage_limit") val usageLimit: Int? = null,
    @Json(name = "used_count") val usedCount: Int = 0,
    @Json(name = "starts_at") val startsAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class MarketingCampaign(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @Json(name = "product_id") val productId: String? = null,
    val platform: String, // Instagram, Facebook, WhatsApp, Telegram, YouTube, Pinterest, Blog
    @Json(name = "tracking_code") val trackingCode: String,
    @Json(name = "destination_url") val destinationUrl: String,
    @Json(name = "clicks_count") val clicksCount: Int = 0,
    @Json(name = "orders_count") val ordersCount: Int = 0,
    @Json(name = "revenue_generated") val revenueGenerated: Double = 0.0,
    @Json(name = "generated_copy") val generatedCopy: String? = null,
    val status: String = "active"
)

@JsonClass(generateAdapter = true)
data class MarketingPost(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "campaign_id") val campaignId: String,
    val platform: String,
    val content: String,
    @Json(name = "post_url") val postUrl: String? = null,
    @Json(name = "scheduled_at") val scheduledAt: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    val status: String = "draft" // draft, scheduled, published, archived
)

@JsonClass(generateAdapter = true)
data class LinkClick(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "campaign_id") val campaignId: String,
    @Json(name = "tracking_code") val trackingCode: String,
    val referrer: String? = null,
    @Json(name = "user_agent") val userAgent: String? = null,
    @Json(name = "ip_hash") val ipHash: String? = null,
    @Json(name = "clicked_at") val clickedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // price_monitor, stock_monitor, content_generation, marketing, order, supplier, notification
    @Json(name = "trigger_desc") val triggerDesc: String? = null,
    @Json(name = "action_desc") val actionDesc: String? = null,
    val enabled: Boolean = true,
    @Json(name = "last_run") val lastRun: String? = null,
    @Json(name = "next_run") val nextRun: String? = null
)

@JsonClass(generateAdapter = true)
data class AutomationRun(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "rule_id") val ruleId: String? = null,
    @Json(name = "rule_name") val ruleName: String = "",
    val status: String = "running", // running, success, failed, cancelled
    @Json(name = "input_data") val inputData: String? = null,
    @Json(name = "output_data") val outputData: String? = null,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "log_output") val logOutput: String = "",
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null,
    @Json(name = "ran_at") val ranAt: String = ""
)

@JsonClass(generateAdapter = true)
data class ProductResearch(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "product_name") val productName: String,
    val source: String,
    @Json(name = "source_price") val sourcePrice: Double,
    @Json(name = "estimated_selling_price") val estimatedSellingPrice: Double,
    @Json(name = "estimated_profit") val estimatedProfit: Double,
    @Json(name = "demand_score") val demandScore: Int = 50,
    @Json(name = "competition_score") val competitionScore: Int = 50,
    @Json(name = "opportunity_score") val opportunityScore: Int = 50,
    @Json(name = "ai_recommendation") val aiRecommendation: String? = null,
    val status: String = "researching" // researching, shortlisted, approved, rejected, imported
)

@JsonClass(generateAdapter = true)
data class StoreSettings(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "store_name") val storeName: String = "Nexus AI Commerce",
    val currency: String = "INR",
    @Json(name = "razorpay_enabled") val razorpayEnabled: Boolean = true,
    @Json(name = "razorpay_page_url") val razorpayPageUrl: String = "https://razorpay.me/@santiramswargiary",
    @Json(name = "razorpay_key_id") val razorpayKeyId: String = "",
    @Json(name = "auto_stock_sync") val autoStockSync: Boolean = true,
    @Json(name = "auto_price_monitor") val autoPriceMonitor: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ActivityLog(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String? = null,
    val action: String,
    @Json(name = "entity_type") val entityType: String? = null,
    @Json(name = "entity_id") val entityId: String? = null,
    val description: String = "",
    val details: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    val timestamp: String = ""
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "notification_type") val notificationType: String = "info", // order, payment, stock, price, alert, info
    val title: String,
    val message: String,
    val type: String = "info",
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    val timestamp: String = ""
)

@JsonClass(generateAdapter = true)
data class ProductReview(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "product_id") val productId: String,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "author_name") val authorName: String = "Customer",
    val rating: Int,
    val title: String? = null,
    val review: String = "",
    val comment: String = "",
    @Json(name = "is_verified_purchase") val isVerifiedPurchase: Boolean = false,
    @Json(name = "verified_purchase") val verifiedPurchase: Boolean = false,
    @Json(name = "is_published") val isPublished: Boolean = false,
    val status: String = "pending", // pending, published, rejected
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteCart(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "session_token") val sessionToken: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteCartItem(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "cart_id") val cartId: String,
    @Json(name = "product_id") val productId: String,
    val quantity: Int = 1,
    val price: Double = 0.0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteWishlistItem(
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "product_id") val productId: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CartItem(
    @Json(name = "product_id") val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    @Json(name = "image_url") val imageUrl: String? = null,
    val sku: String? = null
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    @Json(name = "full_name") val fullName: String,
    val role: String = "customer", // customer, admin, manager, staff
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val phone: String? = null
)

@JsonClass(generateAdapter = true)
data class PriceHistoryEntry(
    @Json(name = "product_id") val productId: String = "",
    @Json(name = "old_price") val oldPrice: Double = 0.0,
    @Json(name = "new_price") val newPrice: Double = 0.0,
    val source: String = "supplier_monitor",
    val date: String = "",
    @Json(name = "recorded_at") val recordedAt: String? = null,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class StockHistoryEntry(
    @Json(name = "product_id") val productId: String = "",
    @Json(name = "old_stock") val oldStock: Int = 0,
    @Json(name = "new_stock") val newStock: Int = 0,
    val reason: String = "",
    val date: String = "",
    @Json(name = "recorded_at") val recordedAt: String? = null
)
