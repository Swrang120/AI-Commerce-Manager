package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.ui.AdminTab
import com.example.ui.MainViewModel
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupabaseStatusCard
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onNavigateTab: (AdminTab) -> Unit,
    onSelectOrder: (Order) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val profitSummary by viewModel.profitSummary.collectAsState()
    val supabaseStatus by viewModel.supabaseStatus.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val lowStockProducts = remember(products) {
        products.filter { it.stockQuantity <= 5 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Supabase Status
        item {
            SupabaseStatusCard(
                status = supabaseStatus,
                onRefresh = { viewModel.testSupabase() }
            )
        }

        // Quick Actions Row
        item {
            Text(text = "Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickActionChip(
                        title = "Import Product",
                        icon = Icons.Default.CloudDownload,
                        accent = Indigo600,
                        onClick = { onNavigateTab(AdminTab.IMPORT) }
                    )
                }
                item {
                    QuickActionChip(
                        title = "AI Research",
                        icon = Icons.Default.AutoAwesome,
                        accent = Violet500,
                        onClick = { onNavigateTab(AdminTab.RESEARCH) }
                    )
                }
                item {
                    QuickActionChip(
                        title = "Manage Orders",
                        icon = Icons.Default.LocalShipping,
                        accent = Emerald500,
                        onClick = { onNavigateTab(AdminTab.ORDERS) }
                    )
                }
                item {
                    QuickActionChip(
                        title = "Automations",
                        icon = Icons.Default.PlayCircleOutline,
                        accent = Amber500,
                        onClick = { onNavigateTab(AdminTab.AUTOMATION) }
                    )
                }
            }
        }

        // Primary KPIs
        item {
            Text(text = "Financial & Store KPIs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Total Revenue",
                    value = "₹${profitSummary.totalRevenue.toInt()}",
                    subtitle = "Gross GMV",
                    icon = Icons.Default.CurrencyRupee,
                    accentColor = Indigo600,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Net Profit",
                    value = "₹${profitSummary.netProfit.toInt()}",
                    subtitle = "${profitSummary.averageMarginPercent.toInt()}% Avg Margin",
                    icon = Icons.Default.TrendingUp,
                    accentColor = Emerald500,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Total Orders",
                    value = orders.size.toString(),
                    subtitle = "${orders.count { it.status == "paid" || it.status == "shipped" }} Fulfilled",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = Violet500,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Catalog Items",
                    value = products.size.toString(),
                    subtitle = "${products.count { it.status == "active" }} Active Drops",
                    icon = Icons.Default.Inventory2,
                    accentColor = Amber500,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Visual Revenue & Profit Performance Bars
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Profit & Cost Structure", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Real-time calculation from active dropshipping orders", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    Spacer(modifier = Modifier.height(14.dp))

                    val rev = profitSummary.totalRevenue.coerceAtLeast(1.0)
                    val costPct = (profitSummary.totalProductCost / rev).toFloat().coerceIn(0f, 1f)
                    val shipPct = (profitSummary.totalShipping / rev).toFloat().coerceIn(0f, 1f)
                    val feePct = (profitSummary.totalPaymentFees / rev).toFloat().coerceIn(0f, 1f)
                    val profitPct = (profitSummary.netProfit / rev).toFloat().coerceIn(0f, 1f)

                    PerformanceBar(label = "Gross Revenue", value = "₹${profitSummary.totalRevenue.toInt()}", percent = 1f, color = Indigo600)
                    PerformanceBar(label = "Supplier COGS (Product Cost)", value = "₹${profitSummary.totalProductCost.toInt()}", percent = costPct, color = Slate600)
                    PerformanceBar(label = "Courier Shipping", value = "₹${profitSummary.totalShipping.toInt()}", percent = shipPct, color = Amber500)
                    PerformanceBar(label = "Net Retained Profit", value = "₹${profitSummary.netProfit.toInt()}", percent = profitPct, color = Emerald500)
                }
            }
        }

        // Low Stock Alert Banner
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Amber50),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Amber700, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Supplier Re-order Alert (${lowStockProducts.size} SKUs Low)", fontWeight = FontWeight.Bold, color = Amber700)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        lowStockProducts.take(2).forEach { prod ->
                            Text(
                                text = "• ${prod.name}: Only ${prod.stockQuantity} units left in stock!",
                                fontSize = 12.sp,
                                color = Slate800
                            )
                        }
                    }
                }
            }
        }

        // Recent Customer Orders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Recent Orders (${orders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onNavigateTab(AdminTab.ORDERS) }) {
                    Text("View All", color = Indigo600)
                }
            }
            if (orders.isEmpty()) {
                Text("No orders placed yet.", color = Slate600, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    orders.take(4).forEach { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectOrder(order) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "#${order.orderNumber} - ${order.customerName}", fontWeight = FontWeight.Bold)
                                    Text(text = "${order.items.size} items • ₹${order.totalAmount.toInt()} via ${order.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = Slate600)
                                }
                                StatusBadge(status = order.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
        }
    }
}

@Composable
fun PerformanceBar(
    label: String,
    value: String,
    percent: Float,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 12.sp, color = Slate700)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent.coerceIn(0.05f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Slate200
        )
    }
}
