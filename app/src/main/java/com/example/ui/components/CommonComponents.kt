package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabaseClient
import com.example.ui.AppViewMode
import com.example.ui.CustomerTab
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreTopBar(
    cartCount: Int,
    wishlistCount: Int,
    currentTab: CustomerTab,
    onNavigateTab: (CustomerTab) -> Unit,
    onSwitchToAdmin: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateTab(CustomerTab.SHOP) }
                    ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Indigo600),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Nexus AI Commerce",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Curated Dropship Drops",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNavigateTab(CustomerTab.SEARCH) },
                        modifier = Modifier.testTag("search_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    BadgedBox(
                        badge = {
                            if (wishlistCount > 0) {
                                Badge(containerColor = Rose500) {
                                    Text(wishlistCount.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { onNavigateTab(CustomerTab.WISHLIST) },
                            modifier = Modifier.testTag("wishlist_icon_button")
                        ) {
                            Icon(
                                imageVector = if (wishlistCount > 0) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = if (wishlistCount > 0) Rose500 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(containerColor = Indigo600) {
                                    Text(cartCount.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { onNavigateTab(CustomerTab.CART) },
                            modifier = Modifier.testTag("cart_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = "Cart",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Switch to Admin Portal Button
                    FilledTonalButton(
                        onClick = onSwitchToAdmin,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Slate100,
                            contentColor = Indigo700
                        ),
                        modifier = Modifier.height(34.dp).testTag("switch_to_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Admin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
                }
        }
    }
}

@Composable
fun AdminTopBar(
    title: String,
    supabaseStatus: SupabaseClient.SupabaseStatus?,
    onSwitchToStore: () -> Unit,
    onTestSupabase: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Surface(
        color = Slate900,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Violet500),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onTestSupabase() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (supabaseStatus?.isConnected == true) Emerald500 else Rose500
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (supabaseStatus?.isConnected == true) "Supabase Live" else "Supabase Offline",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onSwitchToStore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp).testTag("view_store_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Store", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerBottomNav(
    currentTab: CustomerTab,
    cartCount: Int,
    onTabSelected: (CustomerTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val items = listOf(
            Triple(CustomerTab.SHOP, "Shop", Icons.Outlined.Storefront to Icons.Default.Storefront),
            Triple(CustomerTab.CATEGORIES, "Categories", Icons.Outlined.Category to Icons.Default.Category),
            Triple(CustomerTab.CART, "Cart", Icons.Outlined.ShoppingCart to Icons.Default.ShoppingCart),
            Triple(CustomerTab.ORDERS, "Orders", Icons.Outlined.LocalShipping to Icons.Default.LocalShipping),
            Triple(CustomerTab.ACCOUNT, "Account", Icons.Outlined.Person to Icons.Default.Person)
        )

        items.forEach { (tab, label, icons) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == CustomerTab.CART && cartCount > 0) {
                        BadgedBox(
                            badge = { Badge(containerColor = Indigo600) { Text(cartCount.toString()) } }
                        ) {
                            Icon(
                                imageVector = if (isSelected) icons.second else icons.first,
                                contentDescription = label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) icons.second else icons.first,
                            contentDescription = label
                        )
                    }
                },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Indigo600,
                    selectedTextColor = Indigo600,
                    indicatorColor = Indigo50
                )
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    accentColor: Color = Indigo600,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate600
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "active", "paid", "captured", "delivered", "success", "published" ->
            Triple(Emerald50, Emerald700, status.replace("_", " ").uppercase())
        "pending", "supplier_pending", "draft", "candidate" ->
            Triple(Amber50, Amber700, status.replace("_", " ").uppercase())
        "shipped", "ordered", "supplier_ordered", "processing", "shortlisted" ->
            Triple(Indigo50, Indigo700, status.replace("_", " ").uppercase())
        "cancelled", "failed", "out_of_stock", "rejected" ->
            Triple(Rose50, Rose500, status.replace("_", " ").uppercase())
        else -> Triple(Slate100, Slate700, status.uppercase())
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SupabaseStatusCard(
    status: SupabaseClient.SupabaseStatus?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status?.isConnected == true) Emerald50 else Slate100
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (status?.isConnected == true) Emerald500 else Amber500)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Supabase PostgreSQL Backend: ${if (status?.isConnected == true) "Active" else "Checking..."}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = status?.tableStatus ?: "Project: widwrwsjiqbkqpzmuiwe",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh status",
                    tint = Slate700,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
