package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val sku: String?,
    val description: String,
    val shortDescription: String?,
    val categoryId: String?,
    val supplierId: String?,
    val costPrice: Double,
    val shippingCost: Double,
    val desiredProfit: Double,
    val sellingPrice: Double,
    val compareAtPrice: Double?,
    val profitMargin: Double,
    val stockQuantity: Int,
    val status: String,
    val imagesJson: String, // comma or json separated
    val tagsJson: String,
    val seoTitle: String?,
    val seoDescription: String?,
    val categoryName: String?,
    val supplierName: String?,
    val createdAt: String?
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val icon: String?,
    val description: String?,
    val productCount: Int
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val website: String?,
    val type: String,
    val contact: String?,
    val apiStatus: String,
    val status: String,
    val notes: String?,
    val productsCount: Int
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val customerId: String?,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String?,
    val shippingAddress: String,
    val subtotal: Double,
    val shippingCost: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val status: String,
    val paymentStatus: String,
    val paymentMethod: String,
    val razorpayOrderId: String?,
    val razorpayPaymentId: String?,
    val supplierStatus: String,
    val itemsJson: String,
    val createdAt: String?
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String?,
    val sku: String?
)

@Entity(tableName = "wishlist_items")
data class WishlistItemEntity(
    @PrimaryKey val productId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_research")
data class ProductResearchEntity(
    @PrimaryKey val id: String,
    val productName: String,
    val source: String,
    val sourcePrice: Double,
    val estimatedSellingPrice: Double,
    val estimatedProfit: Double,
    val demandScore: Int,
    val competitionScore: Int,
    val opportunityScore: Int,
    val aiRecommendation: String,
    val status: String
)

@Entity(tableName = "marketing_campaigns")
data class MarketingCampaignEntity(
    @PrimaryKey val id: String,
    val name: String,
    val productId: String?,
    val platform: String,
    val trackingCode: String,
    val destinationUrl: String,
    val clicksCount: Int,
    val ordersCount: Int,
    val revenueGenerated: Double,
    val generatedCopy: String?,
    val status: String
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val triggerDesc: String,
    val actionDesc: String,
    val enabled: Boolean,
    val lastRun: String?
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val action: String,
    val details: String,
    val entityType: String?,
    val timestamp: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val timestamp: String
)

@Entity(tableName = "product_reviews")
data class ProductReviewEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val authorName: String,
    val rating: Int,
    val title: String?,
    val comment: String,
    val verifiedPurchase: Boolean,
    val status: String,
    val createdAt: String?
)

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val id: String,
    val code: String,
    val discountType: String,
    val discountValue: Double,
    val minOrderAmount: Double,
    val isActive: Boolean
)
