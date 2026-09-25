package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommerceDao {
    // Products
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :stock WHERE id = :id")
    suspend fun updateStock(id: String, stock: Int)

    @Query("UPDATE products SET sellingPrice = :price, profitMargin = :margin WHERE id = :id")
    suspend fun updatePrice(id: String, price: Double, margin: Double)

    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // Suppliers
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    // Orders
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String)

    @Query("UPDATE orders SET supplierStatus = :supplierStatus WHERE id = :id")
    suspend fun updateSupplierStatus(id: String, supplierStatus: String)

    // Cart
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Wishlist
    @Query("SELECT productId FROM wishlist_items")
    fun getWishlistProductIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWishlist(item: WishlistItemEntity)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun removeFromWishlist(productId: String)

    // Product Research
    @Query("SELECT * FROM product_research ORDER BY opportunityScore DESC")
    fun getAllResearch(): Flow<List<ProductResearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResearch(items: List<ProductResearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResearchItem(item: ProductResearchEntity)

    @Query("UPDATE product_research SET status = :status WHERE id = :id")
    suspend fun updateResearchStatus(id: String, status: String)

    // Marketing Campaigns
    @Query("SELECT * FROM marketing_campaigns ORDER BY clicksCount DESC")
    fun getAllCampaigns(): Flow<List<MarketingCampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaigns(items: List<MarketingCampaignEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(item: MarketingCampaignEntity)

    // Automation Rules
    @Query("SELECT * FROM automation_rules")
    fun getAllAutomationRules(): Flow<List<AutomationRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationRules(rules: List<AutomationRuleEntity>)

    @Query("UPDATE automation_rules SET enabled = :enabled WHERE id = :id")
    suspend fun toggleAutomationRule(id: String, enabled: Boolean)

    @Query("UPDATE automation_rules SET lastRun = :lastRun WHERE id = :id")
    suspend fun updateAutomationLastRun(id: String, lastRun: String)

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun getActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    // Reviews
    @Query("SELECT * FROM product_reviews WHERE productId = :productId AND status = 'published' ORDER BY createdAt DESC")
    fun getReviewsForProduct(productId: String): Flow<List<ProductReviewEntity>>

    @Query("SELECT * FROM product_reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<ProductReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ProductReviewEntity)

    @Query("UPDATE product_reviews SET status = :status WHERE id = :id")
    suspend fun updateReviewStatus(id: String, status: String)

    // Coupons
    @Query("SELECT * FROM coupons WHERE isActive = 1")
    fun getActiveCoupons(): Flow<List<CouponEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupons(coupons: List<CouponEntity>)

    // Automation Runs
    @Query("SELECT * FROM automation_runs ORDER BY ranAt DESC LIMIT 50")
    fun getAutomationRuns(): Flow<List<AutomationRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationRun(run: AutomationRunEntity)

    @Query("SELECT * FROM orders")
    suspend fun getOrdersSync(): List<OrderEntity>

    @Query("SELECT * FROM products")
    suspend fun getProductsSync(): List<ProductEntity>
}
