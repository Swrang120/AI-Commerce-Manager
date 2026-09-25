package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.GeminiService
import com.example.data.remote.ImportResult
import com.example.data.remote.NormalizedProductImport
import com.example.data.remote.SupabaseClient
import com.example.data.remote.VideoRepository
import com.example.data.repository.CommerceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppViewMode {
    CUSTOMER, ADMIN
}

enum class AdminTab {
    DASHBOARD, PRODUCTS, IMPORT, RESEARCH, SUPPLIERS, ORDERS, SHIPMENTS, PAYMENTS, MARKETING, AUTOMATION, AI_CENTER, AI_VIDEOS, PROFIT, COUPONS, REVIEWS, ACTIVITY, SETTINGS
}

enum class CustomerTab {
    SHOP, CATEGORIES, SEARCH, CART, ORDERS, WISHLIST, VIDEOS, ACCOUNT
}

data class ProfitSummary(
    val totalRevenue: Double = 0.0,
    val totalProductCost: Double = 0.0,
    val totalShipping: Double = 0.0,
    val totalPaymentFees: Double = 0.0,
    val netProfit: Double = 0.0,
    val averageMarginPercent: Double = 0.0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = CommerceRepository(application)

    // Current Navigation State
    private val _viewMode = MutableStateFlow(AppViewMode.CUSTOMER)
    val viewMode: StateFlow<AppViewMode> = _viewMode.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminTab.DASHBOARD)
    val adminTab: StateFlow<AdminTab> = _adminTab.asStateFlow()

    private val _customerTab = MutableStateFlow(CustomerTab.SHOP)
    val customerTab: StateFlow<CustomerTab> = _customerTab.asStateFlow()

    // Selected Product for Detail
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategorySlug = MutableStateFlow<String?>(null)
    val selectedCategorySlug: StateFlow<String?> = _selectedCategorySlug.asStateFlow()

    // Coupon
    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _couponMessage = MutableStateFlow<String?>(null)
    val couponMessage: StateFlow<String?> = _couponMessage.asStateFlow()

    // Supabase Connection Status
    private val _supabaseStatus = MutableStateFlow<SupabaseClient.SupabaseStatus?>(null)
    val supabaseStatus: StateFlow<SupabaseClient.SupabaseStatus?> = _supabaseStatus.asStateFlow()

    // User Profile - Real Supabase Auth (No hardcoded admin user!)
    private val _currentUser = MutableStateFlow<UserProfile?>(repository.supabaseClient.currentProfile)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    // Import State
    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // AI Generation States
    private val _aiOptimizedContent = MutableStateFlow<GeminiService.AiProductContent?>(null)
    val aiOptimizedContent: StateFlow<GeminiService.AiProductContent?> = _aiOptimizedContent.asStateFlow()

    private val _aiMarketingBundle = MutableStateFlow<GeminiService.AiMarketingBundle?>(null)
    val aiMarketingBundle: StateFlow<GeminiService.AiMarketingBundle?> = _aiMarketingBundle.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Repository Flows
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistProductIds: StateFlow<List<String>> = repository.wishlistProductIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val researchItems: StateFlow<List<ProductResearch>> = repository.allResearch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val campaigns: StateFlow<List<MarketingCampaign>> = repository.allCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automationRules: StateFlow<List<AutomationRule>> = repository.allAutomationRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLog>> = repository.activityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<ProductReview>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<Coupon>> = repository.activeCoupons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automationRuns: StateFlow<List<AutomationRun>> = repository.allAutomationRuns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val videoRepository = VideoRepository(repository.supabaseClient)
    private val _publicVideos = MutableStateFlow<List<VideoJob>>(emptyList())
    val publicVideos: StateFlow<List<VideoJob>> = _publicVideos.asStateFlow()
    private val _videoLoading = MutableStateFlow(false)
    val videoLoading: StateFlow<Boolean> = _videoLoading.asStateFlow()

    // Profit Calculations Derived Flow
    val profitSummary: StateFlow<ProfitSummary> = orders.map { orderList ->
        var rev = 0.0
        var cost = 0.0
        var ship = 0.0
        for (ord in orderList) {
            if (ord.status == "paid" || ord.status == "shipped" || ord.status == "delivered") {
                rev += ord.totalAmount
                ship += ord.shippingCost
                for (item in ord.items) {
                    cost += item.costPrice * item.quantity
                }
            }
        }
        val paymentFees = rev * 0.02 // 2% Razorpay fee
        val net = rev - cost - ship - paymentFees
        val margin = if (rev > 0) (net / rev) * 100.0 else 0.0
        ProfitSummary(
            totalRevenue = rev,
            totalProductCost = cost,
            totalShipping = ship,
            totalPaymentFees = paymentFees,
            netProfit = net,
            averageMarginPercent = margin
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfitSummary())

    init {
        testSupabase()
        loadPublicVideos()
    }

    fun loadPublicVideos() {
        viewModelScope.launch {
            _videoLoading.value = true
            runCatching { videoRepository.listPublicJobs() }
                .onSuccess { _publicVideos.value = it }
                .also { _videoLoading.value = false }
        }
    }

    // Role-based view switching
    fun setViewMode(mode: AppViewMode, onAccessDenied: ((String) -> Unit)? = null) {
        if (mode == AppViewMode.ADMIN) {
            val user = _currentUser.value
            val role = user?.role?.lowercase() ?: ""
            if (role != "admin" && role != "manager") {
                onAccessDenied?.invoke("Admin access required. Please sign in with an administrator account.")
                return
            }
        }
        _viewMode.value = mode
    }

    fun setAdminTab(tab: AdminTab) {
        _adminTab.value = tab
    }

    fun setCustomerTab(tab: CustomerTab) {
        _customerTab.value = tab
    }

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(slug: String?) {
        _selectedCategorySlug.value = slug
    }

    fun testSupabase() {
        viewModelScope.launch {
            _supabaseStatus.value = repository.supabaseClient.checkConnection()
        }
    }

    // ==========================================
    // AUTHENTICATION METHODS
    // ==========================================

    fun signIn(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.supabaseClient.signIn(email, pass)
            res.fold(
                onSuccess = { profile ->
                    _currentUser.value = profile
                    _authErrorMessage.value = null
                    repository.supabaseClient.mergeGuestCartOnLogin(profile.id)
                    repository.syncFromSupabase()
                    onResult(true, "Welcome back, ${profile.fullName}!")
                },
                onFailure = { err ->
                    _authErrorMessage.value = err.localizedMessage
                    onResult(false, err.localizedMessage ?: "Sign in failed")
                }
            )
        }
    }

    fun signUp(email: String, pass: String, fullName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.supabaseClient.signUp(email, pass, fullName)
            res.fold(
                onSuccess = { msg ->
                    _authErrorMessage.value = null
                    val profile = repository.supabaseClient.currentProfile
                    _currentUser.value = profile
                    if (profile != null) {
                        repository.supabaseClient.mergeGuestCartOnLogin(profile.id)
                    }
                    repository.syncFromSupabase()
                    onResult(true, msg)
                },
                onFailure = { err ->
                    _authErrorMessage.value = err.localizedMessage
                    onResult(false, err.localizedMessage ?: "Sign up failed")
                }
            )
        }
    }

    fun signOut() {
        repository.supabaseClient.clearSession()
        _currentUser.value = null
        _viewMode.value = AppViewMode.CUSTOMER
    }

    // ==========================================
    // STORE CART & ORDER METHODS
    // ==========================================

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product, quantity)
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            repository.removeFromCart(productId)
        }
    }

    fun toggleWishlist(productId: String) {
        viewModelScope.launch {
            val isWishlisted = wishlistProductIds.value.contains(productId)
            repository.toggleWishlist(productId, isWishlisted)
        }
    }

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            val trimmed = code.trim()
            if (trimmed.isBlank()) {
                _appliedCoupon.value = null
                _couponMessage.value = "Please enter a coupon code."
                return@launch
            }
            val subtotal = cartItems.value.sumOf { it.price * it.quantity }
            val res = repository.supabaseClient.validateCouponRemote(trimmed, subtotal)
            if (res.isSuccess) {
                val coupon = res.getOrThrow()
                _appliedCoupon.value = coupon
                val desc = if (coupon.discountType == "percentage") "${coupon.discountValue.toInt()}% off" else "₹${coupon.discountValue.toInt()} off"
                _couponMessage.value = "Coupon '${coupon.code}' applied ($desc)!"
            } else {
                _appliedCoupon.value = null
                _couponMessage.value = res.exceptionOrNull()?.message ?: "Invalid or expired coupon."
            }
        }
    }

    fun placeCustomerOrder(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        address: String,
        paymentMethod: String = "Razorpay",
        onOrderCreated: ((Order) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val items = cartItems.value
            if (items.isEmpty()) {
                onError?.invoke("Cart is empty.")
                return@launch
            }
            try {
                val order = repository.placeOrder(
                    customerName = customerName,
                    customerEmail = customerEmail,
                    customerPhone = customerPhone,
                    address = address,
                    items = items,
                    appliedCoupon = _appliedCoupon.value,
                    paymentMethod = paymentMethod
                )
                _appliedCoupon.value = null
                _couponMessage.value = null
                onOrderCreated?.invoke(order)
                _customerTab.value = CustomerTab.ORDERS
            } catch (e: Exception) {
                onError?.invoke(e.localizedMessage ?: "Order placement failed.")
            }
        }
    }

    fun verifyOrderPayment(orderId: String, paymentId: String, razorpayOrderId: String? = null) {
        viewModelScope.launch {
            repository.recordVerifiedPayment(
                orderId = orderId,
                razorpayPaymentId = paymentId,
                razorpayOrderId = razorpayOrderId
            )
        }
    }

    // ==========================================
    // PRODUCT IMPORT
    // ==========================================

    fun parseSupplierUrl(url: String, source: String) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val res = repository.importService.parseProductUrl(url, source)
                _importResult.value = res
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun confirmImportProduct(
        importData: NormalizedProductImport,
        customPrice: Double? = null,
        customMargin: Double? = null
    ) {
        viewModelScope.launch {
            repository.saveImportedProduct(importData, customPrice, customMargin)
            _importResult.value = null
            _adminTab.value = AdminTab.PRODUCTS
        }
    }

    fun cancelImport() {
        _importResult.value = null
    }

    // ==========================================
    // AI ACTIONS
    // ==========================================

    fun optimizeProductWithAi(productName: String, description: String, price: Double) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val content = repository.geminiService.optimizeProduct(productName, description, price)
                _aiOptimizedContent.value = content
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun generateMarketingCopy(productName: String, price: Double) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val bundle = repository.geminiService.generateMarketingBundle(productName, price)
                _aiMarketingBundle.value = bundle
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun evaluateProductResearch(keyword: String, supplierPrice: Double) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val report = repository.geminiService.analyzeDropshipOpportunity(keyword, supplierPrice)
                val research = ProductResearch(
                    productName = keyword,
                    source = "Market Research",
                    sourcePrice = supplierPrice,
                    estimatedSellingPrice = report.suggestedSellingPrice,
                    estimatedProfit = report.suggestedSellingPrice - supplierPrice - 79.0,
                    demandScore = report.demandScore,
                    competitionScore = report.competitionScore,
                    opportunityScore = report.opportunityScore,
                    aiRecommendation = report.aiRecommendation,
                    status = "researching" // Valid Supabase enum!
                )
                repository.saveResearchCandidate(research)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // ==========================================
    // SUPPLIER & ORDER ACTIONS
    // ==========================================

    fun addSupplier(name: String, website: String, type: String, contact: String, notes: String) {
        viewModelScope.launch {
            repository.addSupplier(
                Supplier(
                    name = name,
                    website = website,
                    type = type,
                    contact = contact,
                    apiStatus = "Configured",
                    status = "active",
                    notes = notes
                )
            )
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
        }
    }

    fun processSupplierFulfillment(orderId: String, supplierName: String, orderNumber: String, trackingNumber: String, courier: String) {
        viewModelScope.launch {
            repository.processSupplierOrder(orderId, supplierName, orderNumber, trackingNumber, courier)
        }
    }

    fun toggleAutomationRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAutomationRule(ruleId, enabled)
        }
    }

    fun runAutomationRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.runAutomationRule(rule)
        }
    }

    fun addProductReview(productId: String, author: String, rating: Int, comment: String, onDone: ((ProductReview) -> Unit)? = null) {
        viewModelScope.launch {
            val customerId = repository.supabaseClient.currentUserId
            val review = repository.addProductReview(productId, customerId, author, rating, comment)
            onDone?.invoke(review)
        }
    }

    fun approveReview(reviewId: String) {
        viewModelScope.launch {
            repository.approveReview(reviewId)
        }
    }

    fun rejectReview(reviewId: String) {
        viewModelScope.launch {
            repository.rejectReview(reviewId)
        }
    }

    fun updateProductPrice(productId: String, price: Double, margin: Double) {
        viewModelScope.launch {
            repository.updateProductPrice(productId, price, margin)
        }
    }

    fun updateProductStock(productId: String, stock: Int) {
        viewModelScope.launch {
            repository.updateProductStock(productId, stock)
        }
    }

    fun createCampaign(name: String, productId: String?, platform: String, destinationUrl: String) {
        viewModelScope.launch {
            val code = platform.take(2).uppercase() + "-" + (1000..9999).random()
            repository.createCampaign(
                MarketingCampaign(
                    name = name,
                    productId = productId,
                    platform = platform,
                    trackingCode = code,
                    destinationUrl = destinationUrl,
                    generatedCopy = "Exclusive Deal: Discover the trending drop on our store!",
                    status = "active"
                )
            )
        }
    }
}
