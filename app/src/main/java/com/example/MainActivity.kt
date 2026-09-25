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
import com.example.ui.theme.Indigo600
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate900
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
                                    AdminTab.MARKETING -> "Marketing & Social"
                                    AdminTab.AUTOMATION -> "Automation Rules"
                                    AdminTab.PROFIT -> "Profit Analytics"
                                    AdminTab.SETTINGS -> "Store Settings"
                                    else -> "Admin Center"
                                },
                                supabaseStatus = supabaseStatus,
                                onSwitchToStore = { viewModel.setViewMode(AppViewMode.CUSTOMER) },
                                onTestSupabase = { viewModel.testSupabase() }
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
                                    AdminTab.MARKETING -> MarketingScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.AUTOMATION -> AutomationScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.PROFIT -> ProfitScreen(
                                        viewModel = viewModel
                                    )
                                    AdminTab.SETTINGS -> SettingsScreen(
                                        viewModel = viewModel
                                    )
                                    else -> AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateTab = { viewModel.setAdminTab(it) },
                                        onSelectOrder = { viewModel.setAdminTab(AdminTab.ORDERS) }
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
fun AdminBottomNavigation(
    currentTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit
) {
    Surface(
        color = Slate900,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val tabs = listOf(
            Triple(AdminTab.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
            Triple(AdminTab.PRODUCTS, "Products", Icons.Default.Inventory2),
            Triple(AdminTab.IMPORT, "Import", Icons.Default.CloudDownload),
            Triple(AdminTab.RESEARCH, "AI Lab", Icons.Default.AutoAwesome),
            Triple(AdminTab.ORDERS, "Orders", Icons.Default.LocalShipping),
            Triple(AdminTab.MARKETING, "Marketing", Icons.Default.Campaign),
            Triple(AdminTab.AUTOMATION, "Rules", Icons.Default.PlayCircleOutline),
            Triple(AdminTab.PROFIT, "Profit", Icons.Default.TrendingUp),
            Triple(AdminTab.SUPPLIERS, "Suppliers", Icons.Default.Business),
            Triple(AdminTab.SETTINGS, "Settings", Icons.Default.Settings)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs) { (tab, label, icon) ->
                val isSelected = currentTab == tab
                Surface(
                    color = if (isSelected) Indigo600 else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onTabSelected(tab) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
