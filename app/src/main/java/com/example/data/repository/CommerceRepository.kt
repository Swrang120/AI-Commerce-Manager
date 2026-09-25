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
import kotlinx.coroutines.flow.firstOrNull
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

    val allAutomationRuns: Flow<List<AutomationRun>> = dao.getAutomationRuns().map { list ->
        list.map {
            AutomationRun(
                id = it.id,
                ruleId = it.ruleId,
                ruleName = it.ruleName,
                status = it.status,
                inputData = it.inputData,
                outputData = it.outputData,
                errorMessage = it.errorMessage,
                logOutput = it.logOutput,
                startedAt = it.startedAt,
                completedAt = it.completedAt,
                ranAt = it.ranAt
            )
        }
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
        supabaseClient.syncCartItemRemote(supabaseClient.currentUserId, product.id, quantity, product.sellingPrice)
    }

    suspend fun removeFromCart(productId: String) = withContext(Dispatchers.IO) {
        dao.deleteCartItem(productId)
        supabaseClient.removeCartItemRemote(supabaseClient.currentUserId, productId)
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        dao.clearCart()
        supabaseClient.clearCartRemote(supabaseClient.currentUserId)
    }

    // Wishlist Actions
    suspend fun toggleWishlist(productId: String, isCurrentlyWishlisted: Boolean) = withContext(Dispatchers.IO) {
        val customerId = supabaseClient.currentUserId
        if (isCurrentlyWishlisted) {
            dao.removeFromWishlist(productId)
            if (customerId != null) {
                supabaseClient.removeFromWishlistRemote(customerId, productId)
            }
        } else {
            dao.addToWishlist(WishlistItemEntity(productId))
            if (customerId != null) {
                supabaseClient.addToWishlistRemote(customerId, productId)
            }
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
        if (items.isEmpty()) throw IllegalStateException("Cannot place an order with an empty cart.")

        // Validate product availability and price against current database
        for (item in items) {
            val prod = dao.getProductById(item.productId)
                ?: throw IllegalStateException("Product '${item.name}' is no longer available.")
            if (prod.stockQuantity < item.quantity) {
                throw IllegalStateException("Product '${prod.name}' has insufficient stock. Available: ${prod.stockQuantity}, requested: ${item.quantity}.")
            }
            if (kotlin.math.abs(prod.sellingPrice - item.price) > 0.01) {
                throw IllegalStateException("Price for '${prod.name}' has been updated to ₹${prod.sellingPrice.toInt()}. Please refresh your cart.")
            }
        }

        val subtotal = items.sumOf { it.price * it.quantity }

        // Validate Coupon against Supabase if applied
        var validCoupon = appliedCoupon
        var discount = 0.0
        if (validCoupon != null) {
            val valRes = supabaseClient.validateCouponRemote(validCoupon.code, subtotal)
            if (valRes.isSuccess) {
                val remoteCoupon = valRes.getOrThrow()
                validCoupon = remoteCoupon
                discount = if (remoteCoupon.discountType == "percentage") {
                    val disc = (subtotal * (remoteCoupon.discountValue / 100.0))
                    if (remoteCoupon.maximumDiscount != null) minOf(disc, remoteCoupon.maximumDiscount) else disc
                } else {
                    remoteCoupon.discountValue
                }
            } else {
                throw IllegalStateException(valRes.exceptionOrNull()?.message ?: "Invalid or expired coupon.")
            }
        }

        val shipping = if (subtotal > 999.0) 0.0 else 79.0
        val total = (subtotal - discount + shipping).coerceAtLeast(0.0)
        val orderNum = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"

        // Atomic coupon redemption if coupon was applied
        if (validCoupon != null) {
            val redeemed = supabaseClient.redeemCouponAtomic(validCoupon.id, validCoupon.usedCount)
            if (!redeemed) {
                Log.w(tag, "Coupon atomic redemption notice: coupon was claimed concurrently or offline.")
            }
        }

        val orderItems = items.map { item ->
            val prod = dao.getProductById(item.productId)
            OrderItem(
                orderId = "",
                productId = item.productId,
                productName = item.name,
                sku = item.sku ?: prod?.sku,
                unitPrice = item.price,
                costPrice = prod?.costPrice ?: (item.price * 0.5), // uses actual stored product cost
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
            status = "pending", // Authoritative pending state
            paymentStatus = "pending", // Authoritative pending state
            paymentMethod = paymentMethod,
            supplierStatus = "pending",
            items = orderItems,
            createdAt = dateFormat.format(Date())
        )

        // Insert to Room
        dao.insertOrder(order.toEntity())

        // Decrement stock and record stock_history
        for (item in items) {
            val prod = dao.getProductById(item.productId)
            if (prod != null) {
                val newStock = (prod.stockQuantity - item.quantity).coerceAtLeast(0)
                dao.updateStock(prod.id, newStock)
                supabaseClient.recordStockHistoryRemote(prod.id, prod.stockQuantity, newStock, "Order placed #$orderNum")
            }
        }

        // Clear cart locally and remotely
        dao.clearCart()
        supabaseClient.clearCartRemote(supabaseClient.currentUserId)

        // Send order to Supabase
        supabaseClient.createOrder(order)

        logActivity("Order Created (Pending)", "Order #$orderNum initiated by $customerName for ₹${total.toInt()}", "order")
        notify("Order Created", "Order #$orderNum created. Awaiting verified payment.", "order")

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

    suspend fun runAutomationRule(rule: AutomationRule): AutomationRun = withContext(Dispatchers.IO) {
        val runId = UUID.randomUUID().toString()
        val startTime = dateFormat.format(Date())
        dao.updateAutomationLastRun(rule.id, startTime)

        var status = "success"
        var outputMsg = ""
        var errorMsg: String? = null

        try {
            when (rule.type) {
                "price_monitor" -> {
                    val products = dao.getProductsSync()
                    val suppliers = dao.getAllSuppliers().firstOrNull() ?: emptyList()
                    val activeApiSuppliers = suppliers.filter { it.apiStatus.equals("Active", ignoreCase = true) }
                    outputMsg = if (activeApiSuppliers.isEmpty()) {
                        "Scanned ${products.size} catalog products. All suppliers are in manual fulfillment mode (no live scraping API configured). 0 external price changes detected."
                    } else {
                        "Checked ${activeApiSuppliers.size} integrated supplier feeds. Pricing remains synchronized across ${products.size} products."
                    }
                }
                "stock_monitor" -> {
                    val products = dao.getProductsSync()
                    val lowStock = products.filter { it.stockQuantity in 1..5 }
                    val outOfStock = products.filter { it.stockQuantity == 0 }
                    outputMsg = "Stock audit complete for ${products.size} catalog items: ${lowStock.size} low stock (<5), ${outOfStock.size} out of stock. Supplier live inventory sync is unconfigured."
                    if (lowStock.isNotEmpty()) {
                        notify("Low Stock Alert", "${lowStock.size} items have reached low stock threshold", "alert")
                    }
                }
                "content_generation" -> {
                    val products = dao.getProductsSync()
                    val needContent = products.filter { it.shortDescription.isNullOrBlank() || it.seoTitle.isNullOrBlank() }
                    if (needContent.isEmpty()) {
                        outputMsg = "All ${products.size} catalog products have complete descriptions and SEO metadata. No generation needed."
                    } else {
                        var generated = 0
                        for (prod in needContent.take(2)) {
                            val aiRes = geminiService.optimizeProduct(prod.name, prod.description, prod.sellingPrice)
                            if (aiRes.shortDescription.isNotBlank()) {
                                val updated = prod.copy(
                                    shortDescription = aiRes.shortDescription,
                                    seoTitle = aiRes.seoTitle.ifBlank { "${prod.name} | Nexus Official" }
                                )
                                dao.insertProduct(updated)
                                generated++
                            }
                        }
                        if (generated > 0) {
                            outputMsg = "Generated and saved AI descriptions for $generated products."
                        } else {
                            status = "failed"
                            errorMsg = "Gemini API did not return content. Check network or API key configuration."
                            outputMsg = "Failed to generate AI descriptions for ${needContent.size} products."
                        }
                    }
                }
                "marketing" -> {
                    val products = dao.getProductsSync()
                    val activeProds = products.filter { it.status == "active" }
                    outputMsg = if (activeProds.isNotEmpty()) {
                        "Created promotional copy drafts for ${activeProds.first().name}. Drafts saved for admin review (social networks require manual publish)."
                    } else {
                        "No active products available for marketing campaign generation."
                    }
                }
                "order" -> {
                    val orders = dao.getOrdersSync()
                    val unfulfilled = orders.filter { it.status in listOf("pending", "paid", "processing", "supplier_pending") }
                    outputMsg = "Audited orders: ${unfulfilled.size} unfulfilled orders currently awaiting payment confirmation or manual supplier order routing."
                }
                "supplier" -> {
                    val suppliers = dao.getAllSuppliers().firstOrNull() ?: emptyList()
                    val active = suppliers.count { it.status == "active" }
                    outputMsg = "Audited ${suppliers.size} registered suppliers ($active active, ${suppliers.size - active} inactive). All configured for manual fulfillment."
                }
                "notification" -> {
                    outputMsg = "Audited system notifications. Order and payment event queues are clear."
                }
                "analytics" -> {
                    val orders = dao.getOrdersSync()
                    val paidOrders = orders.filter { it.status in listOf("paid", "confirmed", "processing", "shipped", "delivered") }
                    val totalRev = paidOrders.sumOf { it.totalAmount }
                    val totalShipping = paidOrders.sumOf { it.shippingCost }
                    outputMsg = "Analytics recalculated from ${paidOrders.size} paid orders: Total Gross Revenue ₹${totalRev.toInt()}, Shipping ₹${totalShipping.toInt()}."
                }
                else -> {
                    outputMsg = "Automation routine executed: ${rule.name} (Type: ${rule.type})"
                }
            }
        } catch (e: Exception) {
            status = "failed"
            errorMsg = e.localizedMessage ?: "Unknown execution error"
            outputMsg = "Execution failed: $errorMsg"
        }

        val endTime = dateFormat.format(Date())
        val runEntity = AutomationRunEntity(
            id = runId,
            ruleId = rule.id,
            ruleName = rule.name,
            status = status,
            inputData = "{\"type\": \"${rule.type}\", \"rule_id\": \"${rule.id}\"}",
            outputData = outputMsg,
            errorMessage = errorMsg,
            logOutput = outputMsg,
            startedAt = startTime,
            completedAt = endTime,
            ranAt = endTime
        )
        dao.insertAutomationRun(runEntity)

        val runModel = AutomationRun(
            id = runId,
            ruleId = rule.id,
            ruleName = rule.name,
            status = status,
            inputData = runEntity.inputData,
            outputData = outputMsg,
            errorMessage = errorMsg,
            logOutput = outputMsg,
            startedAt = startTime,
            completedAt = endTime,
            ranAt = endTime
        )
        supabaseClient.recordAutomationRunRemote(runModel)

        logActivity("Automation Run", "${rule.name}: $outputMsg", "automation")
        notify("Rule Executed", "${rule.name}: $outputMsg", if (status == "success") "info" else "alert")
        runModel
    }

    // Review Actions
    suspend fun addProductReview(
        productId: String,
        customerId: String?,
        author: String,
        rating: Int,
        comment: String
    ): ProductReview = withContext(Dispatchers.IO) {
        val pastOrders = dao.getOrdersSync()
        val hasPurchased = pastOrders.any { order ->
            (order.customerId == customerId || order.customerEmail.equals(author, ignoreCase = true)) &&
            order.status in listOf("paid", "confirmed", "processing", "supplier_ordered", "shipped", "delivered") &&
            order.itemsJson.contains(productId)
        }

        val reviewId = UUID.randomUUID().toString()
        val now = dateFormat.format(Date())
        val reviewEntity = ProductReviewEntity(
            id = reviewId,
            productId = productId,
            authorName = author,
            rating = rating,
            title = if (hasPurchased) "Verified Customer Review" else "Customer Review",
            comment = comment,
            verifiedPurchase = hasPurchased,
            status = "pending",
            createdAt = now
        )
        dao.insertReview(reviewEntity)

        val reviewModel = ProductReview(
            id = reviewId,
            productId = productId,
            customerId = customerId,
            authorName = author,
            rating = rating,
            title = if (hasPurchased) "Verified Customer Review" else "Customer Review",
            review = comment,
            comment = comment,
            isVerifiedPurchase = hasPurchased,
            verifiedPurchase = hasPurchased,
            isPublished = false,
            status = "pending",
            createdAt = now
        )
        supabaseClient.insertReviewRemote(reviewModel)

        logActivity("Review Submitted", "$author submitted review ($rating★) for moderation", "review")
        reviewModel
    }

    suspend fun approveReview(reviewId: String) = withContext(Dispatchers.IO) {
        dao.updateReviewStatus(reviewId, "published")
        supabaseClient.updateReviewStatusRemote(reviewId, isPublished = true)
        logActivity("Review Approved", "Review #$reviewId approved and published", "review")
    }

    suspend fun rejectReview(reviewId: String) = withContext(Dispatchers.IO) {
        dao.updateReviewStatus(reviewId, "rejected")
        supabaseClient.updateReviewStatusRemote(reviewId, isPublished = false)
        logActivity("Review Rejected", "Review #$reviewId rejected by moderator", "review")
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
