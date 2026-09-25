package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Product
import com.example.ui.*
import com.example.ui.admin.*
import com.example.ui.components.AdminTopBar
import com.example.ui.components.CustomerBottomNav
import com.example.ui.components.StoreTopBar
import com.example.ui.customer.*
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val viewMode by viewModel.viewMode.collectAsState()
                val customerTab by viewModel.customerTab.collectAsState()
                val adminTab by viewModel.adminTab.collectAsState()
                val cartItems by viewModel.cartItems.collectAsState()
                val wishlistIds by viewModel.wishlistProductIds.collectAsState()
                val supabaseStatus by viewModel.supabaseStatus.collectAsState()
                val selectedProduct by viewModel.selectedProduct.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        CommerceDrawer(
                            viewMode = viewMode,
                            customerTab = customerTab,
                            adminTab = adminTab,
                            onCustomerTab = {
                                viewModel.setCustomerTab(it)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onAdminTab = {
                                viewModel.setAdminTab(it)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onSwitchMode = {
                                viewModel.setViewMode(it) { msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (viewMode == AppViewMode.CUSTOMER) {
                            StoreTopBar(
                                cartCount = cartItems.sumOf { it.quantity },
                                wishlistCount = wishlistIds.size,
                                currentTab = customerTab,
                                onNavigateTab = { viewModel.setCustomerTab(it) },
                                onOpenMenu = { coroutineScope.launch { drawerState.open() } },
                                onSwitchToAdmin = {
                                    viewModel.setViewMode(AppViewMode.ADMIN) { msg ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                }
                            )
                        } else {
                            AdminTopBar(
                                title = when (adminTab) {
                                    AdminTab.DASHBOARD -> "Commerce Dashboard"
                                    AdminTab.PRODUCTS -> "Product Catalog"
                                    AdminTab.IMPORT -> "Supplier Importer"
                                    AdminTab.RESEARCH -> "AI Product Lab"
                                    AdminTab.SUPPLIERS -> "Supplier Directory"
                                    AdminTab.ORDERS -> "Orders & Fulfillment"
                                    AdminTab.SHIPMENTS -> "Shipments & Tracking"
                                    AdminTab.PAYMENTS -> "Verified Payments"
                                    AdminTab.MARKETING -> "Marketing & Social"
                                    AdminTab.AUTOMATION -> "Automation Rules"
                                    AdminTab.AI_CENTER -> "AI Intelligence Center"
                                    AdminTab.AI_VIDEOS -> "AI Product Videos"
                                    AdminTab.PROFIT -> "Profit Analytics"
                                    AdminTab.COUPONS -> "Coupons & Discounts"
                                    AdminTab.REVIEWS -> "Customer Reviews"
                                    AdminTab.ACTIVITY -> "Activity Logs"
                                    AdminTab.SETTINGS -> "Store Settings"
                                },
                                supabaseStatus = supabaseStatus,
                                onSwitchToStore = { viewModel.setViewMode(AppViewMode.CUSTOMER) },
                                onTestSupabase = { viewModel.testSupabase() },
                                onOpenMenu = { coroutineScope.launch { drawerState.open() } }
                            )
                        }
                    },
                    bottomBar = {
                        if (viewMode == AppViewMode.CUSTOMER) {
                            CustomerBottomNav(
                                currentTab = customerTab,
                                cartCount = cartItems.sumOf { it.quantity },
                                onTabSelected = { viewModel.setCustomerTab(it) }
                            )
                        } else {
                            AdminBottomNavigation(
                                currentTab = adminTab,
                                onTabSelected = { viewModel.setAdminTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Crossfade(targetState = viewMode to (if (viewMode == AppViewMode.CUSTOMER) customerTab.name else adminTab.name)) { (mode, tabName) ->
                            if (mode == AppViewMode.CUSTOMER) {
                                when (customerTab) {
                                    CustomerTab.SHOP -> StoreHomeScreen(
                                        viewModel = viewModel,
                                        onProductClick = { viewModel.selectProduct(it) }
                                    )
                                    CustomerTab.CATEGORIES -> CustomerCategoriesScreen(
                                        viewModel = viewModel,
                                        onCategoryClick = { cat ->
                                            viewModel.selectCategory(cat.slug)
                                            viewModel.setCustomerTab(CustomerTab.SHOP)
                                        }
                                    )
                                    CustomerTab.SEARCH -> CustomerSearchScreen(
                                        viewModel = viewModel,
                                        onProductClick = { viewModel.selectProduct(it) }
                                    )
                                    CustomerTab.CART -> CartScreen(
                                        viewModel = viewModel,
                                        onExploreProducts = { viewModel.setCustomerTab(CustomerTab.SHOP) }
                                    )
                                    CustomerTab.ORDERS -> OrderTrackingScreen(
                                        viewModel = viewModel
                                    )
                                    CustomerTab.WISHLIST -> CustomerWishlistScreen(
                                        viewModel = viewModel,
                                        onProductClick = { viewModel.selectProduct(it) }
                                    )
                                    CustomerTab.VIDEOS -> CustomerVideoLibraryScreen(
                                        viewModel = viewModel
                                    )
                                    CustomerTab.ACCOUNT -> CustomerAccountScreen(
                                        viewModel = viewModel,
                                        onSwitchToAdmin = {
                                            viewModel.setViewMode(AppViewMode.ADMIN) { msg ->
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        }
                                    )
                                }
                            } else {
                                when (adminTab) {
                                    AdminTab.DASHBOARD -> AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateTab = { viewModel.setAdminTab(it) },
                                        onSelectOrder = { viewModel.setAdminTab(AdminTab.ORDERS) }
                                    )
                                    AdminTab.PRODUCTS -> AdminProductsScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.IMPORT -> ProductImportScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.RESEARCH -> ProductResearchScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.SUPPLIERS -> SuppliersScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.ORDERS -> AdminOrdersScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.SHIPMENTS -> AdminShipmentsScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.PAYMENTS -> AdminPaymentsScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.MARKETING -> MarketingScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.AUTOMATION -> AutomationScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.AI_CENTER -> AdminAiCenterScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.AI_VIDEOS -> AdminVideoStudioScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.PROFIT -> ProfitScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.COUPONS -> AdminCouponsScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.REVIEWS -> AdminReviewsScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.ACTIVITY -> AdminActivityScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.SETTINGS -> SettingsScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }

                        // Product Detail Dialog
                        if (selectedProduct != null) {
                            ProductDetailDialog(
                                product = selectedProduct!!,
                                viewModel = viewModel,
                                onDismiss = { viewModel.selectProduct(null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommerceDrawer(
    viewMode: AppViewMode,
    customerTab: CustomerTab,
    adminTab: AdminTab,
    onCustomerTab: (CustomerTab) -> Unit,
    onAdminTab: (AdminTab) -> Unit,
    onSwitchMode: (AppViewMode) -> Unit
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp)) {
            Text(
                if (viewMode == AppViewMode.ADMIN) "AI Commerce Admin" else "AI Commerce Store",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            Divider()
            if (viewMode == AppViewMode.CUSTOMER) {
                val items = listOf(
                    CustomerTab.SHOP to "Home",
                    CustomerTab.CATEGORIES to "Categories",
                    CustomerTab.SEARCH to "Search",
                    CustomerTab.WISHLIST to "Wishlist",
                    CustomerTab.VIDEOS to "AI Product Videos",
                    CustomerTab.CART to "Cart",
                    CustomerTab.ORDERS to "My Orders",
                    CustomerTab.ACCOUNT to "Profile & Settings"
                )
                items.forEach { (tab, label) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = customerTab == tab,
                        onClick = { onCustomerTab(tab) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Admin Panel") },
                    selected = false,
                    onClick = { onSwitchMode(AppViewMode.ADMIN) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            } else {
                val items = listOf(
                    AdminTab.DASHBOARD to "Home / Dashboard",
                    AdminTab.PRODUCTS to "Products",
                    AdminTab.RESEARCH to "Product Research",
                    AdminTab.IMPORT to "AI Product Import",
                    AdminTab.SUPPLIERS to "Suppliers",
                    AdminTab.ORDERS to "Orders",
                    AdminTab.SHIPMENTS to "Shipments",
                    AdminTab.PAYMENTS to "Payments",
                    AdminTab.AI_VIDEOS to "AI Video Studio",
                    AdminTab.MARKETING to "Marketing",
                    AdminTab.AUTOMATION to "Automation",
                    AdminTab.AI_CENTER to "AI Intelligence",
                    AdminTab.PROFIT to "Analytics / Earnings",
                    AdminTab.COUPONS to "Coupons",
                    AdminTab.REVIEWS to "Reviews",
                    AdminTab.ACTIVITY to "Activity Logs",
                    AdminTab.SETTINGS to "Settings"
                )
                items.forEach { (tab, label) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = adminTab == tab,
                        onClick = { onAdminTab(tab) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Customer Store") },
                    selected = false,
                    onClick = { onSwitchMode(AppViewMode.CUSTOMER) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AdminBottomNavigation(
    currentTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit
) {
    var showAllModulesDialog by remember { mutableStateOf(false) }

    Surface(
        color = Slate900,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBar(
            containerColor = Slate900,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentTab == AdminTab.DASHBOARD,
                onClick = { onTabSelected(AdminTab.DASHBOARD) },
                icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Overview", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Indigo600,
                    unselectedIconColor = Slate400,
                    unselectedTextColor = Slate400
                )
            )
            NavigationBarItem(
                selected = currentTab == AdminTab.PRODUCTS || currentTab == AdminTab.IMPORT,
                onClick = { onTabSelected(AdminTab.PRODUCTS) },
                icon = { Icon(imageVector = Icons.Default.Inventory2, contentDescription = "Products") },
                label = { Text("Catalog", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Indigo600,
                    unselectedIconColor = Slate400,
                    unselectedTextColor = Slate400
                )
            )
            NavigationBarItem(
                selected = currentTab == AdminTab.ORDERS || currentTab == AdminTab.SHIPMENTS,
                onClick = { onTabSelected(AdminTab.ORDERS) },
                icon = { Icon(imageVector = Icons.Default.LocalShipping, contentDescription = "Orders") },
                label = { Text("Orders", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Indigo600,
                    unselectedIconColor = Slate400,
                    unselectedTextColor = Slate400
                )
            )
            NavigationBarItem(
                selected = currentTab == AdminTab.AUTOMATION || currentTab == AdminTab.AI_CENTER || currentTab == AdminTab.AI_VIDEOS || currentTab == AdminTab.RESEARCH,
                onClick = { onTabSelected(AdminTab.AUTOMATION) },
                icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Automation") },
                label = { Text("Automation", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Indigo600,
                    unselectedIconColor = Slate400,
                    unselectedTextColor = Slate400
                )
            )
            NavigationBarItem(
                selected = showAllModulesDialog || (currentTab !in listOf(AdminTab.DASHBOARD, AdminTab.PRODUCTS, AdminTab.ORDERS, AdminTab.AUTOMATION)),
                onClick = { showAllModulesDialog = true },
                icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "More") },
                label = { Text("More", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Indigo600,
                    unselectedIconColor = Slate400,
                    unselectedTextColor = Slate400
                )
            )
        }
    }

    if (showAllModulesDialog) {
        val allModules = listOf(
            Triple(AdminTab.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
            Triple(AdminTab.PRODUCTS, "Product Catalog", Icons.Default.Inventory2),
            Triple(AdminTab.IMPORT, "Supplier Import", Icons.Default.CloudDownload),
            Triple(AdminTab.RESEARCH, "AI Product Lab", Icons.Default.AutoAwesome),
            Triple(AdminTab.SUPPLIERS, "Suppliers", Icons.Default.Business),
            Triple(AdminTab.ORDERS, "Orders & Routing", Icons.Default.ReceiptLong),
            Triple(AdminTab.SHIPMENTS, "Shipments & Tracking", Icons.Default.LocalShipping),
            Triple(AdminTab.PAYMENTS, "Verified Payments", Icons.Default.CurrencyRupee),
            Triple(AdminTab.MARKETING, "Marketing Campaigns", Icons.Default.Campaign),
            Triple(AdminTab.AUTOMATION, "Automation Rules", Icons.Default.PlayCircleOutline),
            Triple(AdminTab.AI_CENTER, "AI Intelligence", Icons.Default.Psychology),
            Triple(AdminTab.AI_VIDEOS, "AI Product Videos", Icons.Default.VideoLibrary),
            Triple(AdminTab.PROFIT, "Profit Statement", Icons.Default.TrendingUp),
            Triple(AdminTab.COUPONS, "Coupons & Codes", Icons.Default.Discount),
            Triple(AdminTab.REVIEWS, "Customer Reviews", Icons.Default.RateReview),
            Triple(AdminTab.ACTIVITY, "Activity Audit", Icons.Default.History),
            Triple(AdminTab.SETTINGS, "Store Settings", Icons.Default.Settings)
        )

        Dialog(onDismissRequest = { showAllModulesDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "All Admin Modules (16)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAllModulesDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Divider(color = Slate200, modifier = Modifier.padding(vertical = 8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 420.dp)
                    ) {
                        items(allModules) { (tab, label, icon) ->
                            val isSelected = currentTab == tab
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Indigo50 else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onTabSelected(tab)
                                        showAllModulesDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Indigo600 else Slate600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Indigo600 else Slate800,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
