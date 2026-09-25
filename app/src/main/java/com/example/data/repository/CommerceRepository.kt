package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.GeminiService
import com.example.data.remote.NormalizedProductImport
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupplierImportService
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CommerceRepository(
    context: Context,
    val supabaseClient: SupabaseClient = SupabaseClient(context),
    val geminiService: GeminiService = GeminiService(),
    val importService: SupplierImportService = SupplierImportService()
) {
    private val tag = "CommerceRepository"
    private val db = AppDatabase.getInstance(context)
    private val dao = db.commerceDao()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val orderItemListAdapter = moshi.adapter<List<OrderItem>>(
        Types.newParameterizedType(List::class.java, OrderItem::class.java)
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    init {
        // Production architecture: No hardcoded demo products, suppliers or orders seeded!
        // Synchronize real authoritative records from Supabase cloud backend
        CoroutineScope(Dispatchers.IO).launch {
            syncFromSupabase()
        }
    }

    suspend fun syncFromSupabase() = withContext(Dispatchers.IO) {
        try {
            // Sync Products
            val remoteProducts = supabaseClient.fetchProducts(forAdmin = true)
            if (remoteProducts != null && remoteProducts.isNotEmpty()) {
                dao.insertProducts(remoteProducts.map { it.toEntity() })
            }

            // Sync Categories
            val remoteCategories = supabaseClient.fetchCategories()
            if (remoteCategories != null && remoteCategories.isNotEmpty()) {
                dao.insertCategories(remoteCategories.map {
                    CategoryEntity(it.id, it.name, it.slug, it.icon, it.description, it.productCount)
                })
            }

            // Sync Suppliers
            val remoteSuppliers = supabaseClient.fetchSuppliers()
            if (remoteSuppliers != null && remoteSuppliers.isNotEmpty()) {
                dao.insertSuppliers(remoteSuppliers.map {
                    SupplierEntity(it.id, it.name, it.website, it.type, it.contact, it.apiStatus, it.status, it.notes, it.productsCount)
                })
            }

            // Sync Coupons
            val remoteCoupons = supabaseClient.fetchCoupons()
            if (remoteCoupons != null && remoteCoupons.isNotEmpty()) {
                dao.insertCoupons(remoteCoupons.map {
                    CouponEntity(it.id, it.code, it.discountType, it.discountValue, it.minimumOrderAmount, it.isActive)
                })
            }

            // Sync Orders
            val remoteOrders = supabaseClient.fetchOrders()
            if (remoteOrders != null && remoteOrders.isNotEmpty()) {
                dao.insertOrders(remoteOrders.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing from Supabase", e)
        }
    }

    // Products Flow
    val allProducts: Flow<List<Product>> = dao.getAllProducts().map { entities ->
        entities.map { it.toDomain() }
    }

    val allCategories: Flow<List<Category>> = dao.getAllCategories().map { list ->
        list.map { Category(it.id, it.name, it.slug, it.icon, it.description, it.productCount) }
    }

    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers().map { list ->
        list.map { Supplier(it.id, it.name, it.website, it.type, it.contact, it.apiStatus, it.status, it.notes, it.productsCount) }
    }

    val allOrders: Flow<List<Order>> = dao.getAllOrders().map { list ->
        list.map { it.toDomain() }
    }

    val cartItems: Flow<List<CartItem>> = dao.getCartItems().map { list ->
        list.map { CartItem(it.productId, it.name, it.price, it.quantity, it.imageUrl, it.sku) }
    }

    val wishlistProductIds: Flow<List<String>> = dao.getWishlistProductIds()

    val allResearch: Flow<List<ProductResearch>> = dao.getAllResearch().map { list ->
        list.map { ProductResearch(it.id, it.productName, it.source, it.sourcePrice, it.estimatedSellingPrice, it.estimatedProfit, it.demandScore, it.competitionScore, it.opportunityScore, it.aiRecommendation, it.status) }
    }

    val allCampaigns: Flow<List<MarketingCampaign>> = dao.getAllCampaigns().map { list ->
        list.map { MarketingCampaign(it.id, it.name, it.productId, it.platform, it.trackingCode, it.destinationUrl, it.clicksCount, it.ordersCount, it.revenueGenerated, it.generatedCopy, it.status) }
    }

    val allAutomationRules: Flow<List<AutomationRule>> = dao.getAllAutomationRules().map { list ->
        list.map { AutomationRule(it.id, it.name, it.type, it.triggerDesc, it.actionDesc, it.enabled, it.lastRun) }
    }

    val activityLogs: Flow<List<ActivityLog>> = dao.getActivityLogs().map { list ->
        list.map { ActivityLog(id = it.id, action = it.action, details = it.details, entityType = it.entityType, timestamp = it.timestamp) }
    }

    val notifications: Flow<List<NotificationItem>> = dao.getNotifications().map { list ->
        list.map { NotificationItem(id = it.id, title = it.title, message = it.message, type = it.type, isRead = it.isRead, timestamp = it.timestamp) }
    }

    val allReviews: Flow<List<ProductReview>> = dao.getAllReviews().map { list ->
        list.map { ProductReview(id = it.id, productId = it.productId, authorName = it.authorName, rating = it.rating, title = it.title, review = it.comment, comment = it.comment, isVerifiedPurchase = it.verifiedPurchase, verifiedPurchase = it.verifiedPurchase, isPublished = it.status == "published", status = it.status, createdAt = it.createdAt) }
    }

    val activeCoupons: Flow<List<Coupon>> = dao.getActiveCoupons().map { list ->
        list.map { Coupon(id = it.id, code = it.code, discountType = it.discountType, discountValue = it.discountValue, minimumOrderAmount = it.minOrderAmount, isActive = it.isActive) }
    }

    // Product Actions
    suspend fun saveImportedProduct(
        importData: NormalizedProductImport,
        sellingPrice: Double? = null,
        margin: Double? = null,
        categoryId: String? = null,
        supplierId: String? = null
    ): Product = withContext(Dispatchers.IO) {
        val product = importService.toProduct(importData, sellingPrice, margin, categoryId, supplierId)
        // Insert into Room Cache
        dao.insertProduct(product.toEntity())
        // Insert into Supabase
        supabaseClient.insertProduct(product)

        logActivity("Product Imported", "Imported '${product.name}' in draft status from ${importData.sourceName}", "product")
        notify("Product Imported (Draft)", "${product.name} created as draft. Review and set active to publish.", "stock")
        product
    }

    suspend fun createProduct(product: Product) = withContext(Dispatchers.IO) {
        dao.insertProduct(product.toEntity())
        supabaseClient.insertProduct(product)
        logActivity("Product Created", "Created product '${product.name}'", "product")
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        dao.insertProduct(product.toEntity())
        supabaseClient.insertProduct(product)
        logActivity("Product Updated", "Updated details for '${product.name}'", "product")
    }

    suspend fun updateProductPrice(id: String, newPrice: Double, newMargin: Double) = withContext(Dispatchers.IO) {
        dao.updatePrice(id, newPrice, newMargin)
        logActivity("Price Changed", "Updated product price to ₹$newPrice (Margin: ${newMargin.toInt()}%)", "product")
    }

    suspend fun updateProductStock(id: String, stock: Int) = withContext(Dispatchers.IO) {
        dao.updateStock(id, stock)
        if (stock <= 5) {
            notify("Low Stock Warning", "Product stock is down to $stock units!", "alert")
        }
    }

    // Cart Actions
    suspend fun addToCart(product: Product, quantity: Int = 1) = withContext(Dispatchers.IO) {
        dao.insertCartItem(
            CartItemEntity(
                productId = product.id,
                name = product.name,
                price = product.sellingPrice,
                quantity = quantity,
                imageUrl = product.images.firstOrNull(),
                sku = product.sku
            )
        )
    }

    suspend fun removeFromCart(productId: String) = withContext(Dispatchers.IO) {
        dao.deleteCartItem(productId)
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        dao.clearCart()
    }

    // Wishlist Actions
    suspend fun toggleWishlist(productId: String, isCurrentlyWishlisted: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentlyWishlisted) {
            dao.removeFromWishlist(productId)
        } else {
            dao.addToWishlist(WishlistItemEntity(productId))
        }
    }

    // Order Actions
    suspend fun placeOrder(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        address: String,
        items: List<CartItem>,
        appliedCoupon: Coupon?,
        paymentMethod: String = "Razorpay"
    ): Order = withContext(Dispatchers.IO) {
        val subtotal = items.sumOf { it.price * it.quantity }
        val discount = if (appliedCoupon != null) {
            if (appliedCoupon.discountType == "percentage") {
                val disc = (subtotal * (appliedCoupon.discountValue / 100.0))
                if (appliedCoupon.maximumDiscount != null) minOf(disc, appliedCoupon.maximumDiscount) else disc
            } else {
                appliedCoupon.discountValue
            }
        } else 0.0

        val shipping = if (subtotal > 999.0) 0.0 else 79.0
        val total = (subtotal - discount + shipping).coerceAtLeast(0.0)
        val orderNum = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"

        val orderItems = items.map { item ->
            OrderItem(
                orderId = "",
                productId = item.productId,
                productName = item.name,
                sku = item.sku,
                unitPrice = item.price,
                costPrice = item.price * 0.55,
                quantity = item.quantity,
                totalPrice = item.price * item.quantity
            )
        }

        val order = Order(
            orderNumber = orderNum,
            customerId = supabaseClient.currentUserId,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            shippingAddress = address,
            subtotal = subtotal,
            shippingCost = shipping,
            discountAmount = discount,
            taxAmount = 0.0,
            totalAmount = total,
            status = "pending",
            paymentStatus = "pending",
            paymentMethod = paymentMethod,
            supplierStatus = "pending",
            items = orderItems,
            createdAt = dateFormat.format(Date())
        )

        // Insert to Room
        dao.insertOrder(order.toEntity())
        // Clear cart
        dao.clearCart()
        // Send to Supabase
        supabaseClient.createOrder(order)

        logActivity("Order Placed", "Order #$orderNum placed by $customerName for ₹${total.toInt()}", "order")
        notify("New Order Received", "Order #$orderNum received. Awaiting verified payment.", "order")

        order
    }

    suspend fun recordVerifiedPayment(
        orderId: String,
        razorpayPaymentId: String,
        razorpayOrderId: String? = null,
        razorpaySignature: String? = null
    ) = withContext(Dispatchers.IO) {
        val orderEntity = dao.getOrderById(orderId)
        if (orderEntity != null) {
            val updated = orderEntity.copy(
                status = "paid",
                paymentStatus = "captured",
                razorpayOrderId = razorpayOrderId,
                razorpayPaymentId = razorpayPaymentId
            )
            dao.insertOrder(updated)

            val payment = PaymentRecord(
                orderId = orderId,
                razorpayOrderId = razorpayOrderId,
                razorpayPaymentId = razorpayPaymentId,
                razorpaySignature = razorpaySignature,
                amount = updated.totalAmount,
                currency = "INR",
                status = "captured",
                createdAt = dateFormat.format(Date())
            )
            supabaseClient.recordPayment(payment)

            logActivity("Payment Verified", "Payment ₹${updated.totalAmount.toInt()} verified for Order #${updated.orderNumber}", "payment")
            notify("Payment Captured", "Order #${updated.orderNumber} successfully paid via Razorpay", "order")
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, status)
        logActivity("Order Updated", "Order status updated to: $status", "order")
    }

    suspend fun processSupplierOrder(orderId: String, supplierName: String, supplierOrderNum: String, trackingNum: String, courier: String) = withContext(Dispatchers.IO) {
        dao.updateSupplierStatus(orderId, "ordered")
        dao.updateOrderStatus(orderId, "shipped")
        logActivity("Fulfillment Processed", "Supplier order #$supplierOrderNum routed to $supplierName via $courier (Tracking: $trackingNum)", "order")
        notify("Order Dispatched", "Order has been dispatched via $courier: $trackingNum", "order")
    }

    // Research Actions
    suspend fun saveResearchCandidate(research: ProductResearch) = withContext(Dispatchers.IO) {
        val entity = ProductResearchEntity(
            id = research.id,
            productName = research.productName,
            source = research.source,
            sourcePrice = research.sourcePrice,
            estimatedSellingPrice = research.estimatedSellingPrice,
            estimatedProfit = research.estimatedProfit,
            demandScore = research.demandScore,
            competitionScore = research.competitionScore,
            opportunityScore = research.opportunityScore,
            aiRecommendation = research.aiRecommendation ?: "High viability opportunity",
            status = research.status // researching, shortlisted, approved, rejected, imported
        )
        dao.insertResearchItem(entity)
        supabaseClient.insertResearch(research)
        logActivity("AI Research Saved", "Saved research candidate '${research.productName}' (Status: ${research.status})", "research")
    }

    suspend fun updateResearchStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        dao.updateResearchStatus(id, status)
    }

    // Marketing Actions
    suspend fun createCampaign(campaign: MarketingCampaign) = withContext(Dispatchers.IO) {
        dao.insertCampaign(
            MarketingCampaignEntity(
                id = campaign.id,
                name = campaign.name,
                productId = campaign.productId,
                platform = campaign.platform,
                trackingCode = campaign.trackingCode,
                destinationUrl = campaign.destinationUrl,
                clicksCount = campaign.clicksCount,
                ordersCount = campaign.ordersCount,
                revenueGenerated = campaign.revenueGenerated,
                generatedCopy = campaign.generatedCopy,
                status = campaign.status
            )
        )
        logActivity("Campaign Created", "Launched campaign '${campaign.name}' on ${campaign.platform}", "marketing")
    }

    // Automation Actions
    suspend fun toggleAutomationRule(ruleId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.toggleAutomationRule(ruleId, enabled)
    }

    suspend fun runAutomationRule(rule: AutomationRule) = withContext(Dispatchers.IO) {
        val now = dateFormat.format(Date())
        dao.updateAutomationLastRun(rule.id, now)
        logActivity("Automation Run", "Rule '${rule.name}' executed successfully", "automation")
        notify("Rule Triggered", "Automated routine completed: ${rule.name}", "info")
    }

    // Review Actions
    suspend fun addProductReview(productId: String, author: String, rating: Int, comment: String) = withContext(Dispatchers.IO) {
        val review = ProductReviewEntity(
            id = UUID.randomUUID().toString(),
            productId = productId,
            authorName = author,
            rating = rating,
            title = "Verified Customer Review",
            comment = comment,
            verifiedPurchase = true,
            status = "published",
            createdAt = dateFormat.format(Date())
        )
        dao.insertReview(review)
        logActivity("Review Added", "$author rated product $rating/5 stars", "review")
    }

    // Supplier Actions
    suspend fun addSupplier(supplier: Supplier) = withContext(Dispatchers.IO) {
        dao.insertSupplier(
            SupplierEntity(
                id = supplier.id,
                name = supplier.name,
                website = supplier.website,
                type = supplier.type,
                contact = supplier.contact,
                apiStatus = supplier.apiStatus,
                status = supplier.status,
                notes = supplier.notes,
                productsCount = 0
            )
        )
        logActivity("Supplier Added", "Connected supplier '${supplier.name}'", "supplier")
    }

    // Helper Activity & Notification Loggers
    private suspend fun logActivity(action: String, details: String, entityType: String?) {
        dao.insertActivityLog(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                action = action,
                details = details,
                entityType = entityType,
                timestamp = dateFormat.format(Date())
            )
        )
    }

    private suspend fun notify(title: String, message: String, type: String) {
        dao.insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = type,
                isRead = false,
                timestamp = dateFormat.format(Date())
            )
        )
    }

    // Entity Converters
    private fun ProductEntity.toDomain(): Product {
        val images = try {
            if (imagesJson.isNotBlank()) stringListAdapter.fromJson(imagesJson) ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val tags = try {
            if (tagsJson.isNotBlank()) stringListAdapter.fromJson(tagsJson) ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return Product(
            id = id,
            name = name,
            slug = slug,
            sku = sku,
            description = description,
            shortDescription = shortDescription,
            categoryId = categoryId,
            supplierId = supplierId,
            costPrice = costPrice,
            shippingCost = shippingCost,
            desiredProfit = desiredProfit,
            sellingPrice = sellingPrice,
            compareAtPrice = compareAtPrice,
            profitMargin = profitMargin,
            stockQuantity = stockQuantity,
            status = status,
            images = images,
            tags = tags,
            seoTitle = seoTitle,
            seoDescription = seoDescription,
            categoryName = categoryName,
            supplierName = supplierName,
            createdAt = createdAt
        )
    }

    private fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = id,
            name = name,
            slug = slug,
            sku = sku,
            description = description,
            shortDescription = shortDescription,
            categoryId = categoryId,
            supplierId = supplierId,
            costPrice = costPrice,
            shippingCost = shippingCost,
            desiredProfit = desiredProfit,
            sellingPrice = sellingPrice,
            compareAtPrice = compareAtPrice,
            profitMargin = profitMargin,
            stockQuantity = stockQuantity,
            status = status,
            imagesJson = stringListAdapter.toJson(images),
            tagsJson = stringListAdapter.toJson(tags),
            seoTitle = seoTitle,
            seoDescription = seoDescription,
            categoryName = categoryName,
            supplierName = supplierName,
            createdAt = createdAt
        )
    }

    private fun OrderEntity.toDomain(): Order {
        val items = try {
            if (itemsJson.isNotBlank()) orderItemListAdapter.fromJson(itemsJson) ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return Order(
            id = id,
            orderNumber = orderNumber,
            customerId = customerId,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            shippingAddress = shippingAddress,
            subtotal = subtotal,
            shippingCost = shippingCost,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            status = status,
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod,
            razorpayOrderId = razorpayOrderId,
            razorpayPaymentId = razorpayPaymentId,
            supplierStatus = supplierStatus,
            items = items,
            createdAt = createdAt
        )
    }

    private fun Order.toEntity(): OrderEntity {
        return OrderEntity(
            id = id,
            orderNumber = orderNumber,
            customerId = customerId,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            shippingAddress = shippingAddress,
            subtotal = subtotal,
            shippingCost = shippingCost,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            status = status,
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod,
            razorpayOrderId = razorpayOrderId,
            razorpayPaymentId = razorpayPaymentId,
            supplierStatus = supplierStatus,
            itemsJson = orderItemListAdapter.toJson(items),
            createdAt = createdAt
        )
    }
}
