package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupabaseStatusCard
import com.example.ui.theme.*

@Composable
fun AdminProductsScreen(viewModel: MainViewModel) {
    val products by viewModel.products.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var productForStockEdit by remember { mutableStateOf<Product?>(null) }

    val filtered = remember(products, selectedFilter) {
        when (selectedFilter) {
            "Active" -> products.filter { it.status == "active" }
            "Low Stock" -> products.filter { it.stockQuantity <= 5 }
            else -> products
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_products_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "Catalog & Inventory Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Manage dropship products, pricing margins, stock counts, and AI-optimized listings.", style = MaterialTheme.typography.bodySmall, color = Slate600)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Active", "Low Stock").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Outlined.Inventory2, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Your catalog is empty.", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Your catalog is empty. Add or import products to start selling.", fontSize = 12.sp, color = Slate600)
                        }
                    }
                }
            }
        } else {
            items(filtered) { product ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedProductForDetail = product },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (product.images.isNotEmpty()) {
                        AsyncImage(
                            model = product.images.first(),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = product.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Cost: ₹${product.costPrice.toInt()} • Selling: ₹${product.sellingPrice.toInt()}", fontSize = 12.sp, color = Slate600)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(status = product.status)
                            Text(
                                text = "Stock: ${product.stockQuantity}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (product.stockQuantity <= 5) Rose500 else Emerald700
                            )
                        }
                    }
                    IconButton(onClick = { productForStockEdit = product }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Stock", tint = Indigo600)
                    }
                }
            }
        }
        }
    }

    if (productForStockEdit != null) {
        val prod = productForStockEdit!!
        var newStockText by remember { mutableStateOf(prod.stockQuantity.toString()) }
        var newPriceText by remember { mutableStateOf(prod.sellingPrice.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { productForStockEdit = null },
            title = { Text("Update Price & Stock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newStockText,
                        onValueChange = { newStockText = it },
                        label = { Text("Stock Quantity") }
                    )
                    OutlinedTextField(
                        value = newPriceText,
                        onValueChange = { newPriceText = it },
                        label = { Text("Selling Price (INR)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val stock = newStockText.toIntOrNull() ?: prod.stockQuantity
                    val price = newPriceText.toDoubleOrNull() ?: prod.sellingPrice
                    val margin = if (price > 0) ((price - prod.costPrice - prod.shippingCost) / price * 100.0) else 0.0
                    viewModel.updateProductStock(prod.id, stock)
                    viewModel.updateProductPrice(prod.id, price, margin)
                    productForStockEdit = null
                }) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { productForStockEdit = null }) { Text("Cancel") }
            }
        )
    }

    if (selectedProductForDetail != null) {
        AdminProductDetailDialog(
            product = selectedProductForDetail!!,
            viewModel = viewModel,
            onDismiss = { selectedProductForDetail = null }
        )
    }
}

@Composable
fun AdminProductDetailDialog(
    product: Product,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Pricing & Margin", "SEO & Copy", "Supplier")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Product Inspector", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }

                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> { // Overview
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Text(text = product.name, fontWeight = FontWeight.Bold) }
                            item { Text(text = "SKU: ${product.sku ?: "N/A"}", color = Slate600, fontSize = 12.sp) }
                            item { Text(text = "Category: ${product.categoryName ?: "General"}", color = Slate600, fontSize = 12.sp) }
                            item { Text(text = product.description, style = MaterialTheme.typography.bodySmall, color = Slate700) }
                        }
                    }
                    1 -> { // Pricing
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Supplier Cost: ₹${product.costPrice.toInt()}", fontWeight = FontWeight.SemiBold)
                            Text(text = "Courier Shipping: ₹${product.shippingCost.toInt()}", fontWeight = FontWeight.SemiBold)
                            Text(text = "Selling Price: ₹${product.sellingPrice.toInt()}", fontWeight = FontWeight.Bold, color = Indigo600)
                            Text(text = "Profit Margin: ${product.profitMargin.toInt()}%", fontWeight = FontWeight.Bold, color = Emerald700)
                        }
                    }
                    2 -> { // SEO & Copy
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "SEO Title:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = product.seoTitle ?: product.name, fontSize = 12.sp, color = Slate700)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "SEO Description:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = product.seoDescription ?: product.description.take(150), fontSize = 12.sp, color = Slate700)
                        }
                    }
                    3 -> { // Supplier
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Supplier Partner: ${product.supplierName ?: "Amazon / Meesho Verified"}", fontWeight = FontWeight.Bold)
                            Text(text = "Fulfillment SLA: 3-5 business days dispatch", fontSize = 12.sp, color = Slate600)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuppliersScreen(viewModel: MainViewModel) {
    val suppliers by viewModel.suppliers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_suppliers_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Supplier Network (${suppliers.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Manage wholesale partners, direct manufacturers, and APIs.", style = MaterialTheme.typography.bodySmall, color = Slate600)
                }
                Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(8.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        items(suppliers) { sup ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = sup.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        StatusBadge(status = sup.apiStatus)
                    }
                    Text(text = "Type: ${sup.type} • Contact: ${sup.contact ?: "N/A"}", fontSize = 12.sp, color = Slate600)
                    if (!sup.notes.isNullOrBlank()) {
                        Text(text = sup.notes ?: "", fontSize = 11.sp, color = Slate400)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var website by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Marketplace") }
        var contact by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name") })
                    OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") })
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Email / Phone") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes / Terms") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addSupplier(name, website, type, contact, notes)
                        showAddDialog = false
                    }
                }) {
                    Text("Save Supplier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminOrdersScreen(viewModel: MainViewModel) {
    val orders by viewModel.orders.collectAsState()
    var orderForFulfillment by remember { mutableStateOf<Order?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_orders_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "Orders & Fulfillment (${orders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Process customer dropship orders, dispatch supplier purchase orders, and track courier dispatches.", style = MaterialTheme.typography.bodySmall, color = Slate600)
        }

        items(orders) { order ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Order #${order.orderNumber}", fontWeight = FontWeight.Bold)
                            Text(text = order.customerName, fontSize = 12.sp, color = Slate600)
                        }
                        StatusBadge(status = order.status)
                    }

                    order.items.forEach { item ->
                        Text(text = "• ${item.quantity}x ${item.productName} (₹${item.totalPrice.toInt()})", fontSize = 12.sp)
                    }

                    Divider(color = Slate100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total: ₹${order.totalAmount.toInt()}", fontWeight = FontWeight.ExtraBold, color = Indigo600)
                        if (order.status != "shipped" && order.status != "delivered") {
                            Button(
                                onClick = { orderForFulfillment = order },
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Fulfill via Supplier", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (orderForFulfillment != null) {
        val ord = orderForFulfillment!!
        var supplierName by remember { mutableStateOf("Amazon Wholesalers Hub") }
        var supplierOrderNo by remember { mutableStateOf("SUP-" + (100000..999999).random()) }
        var courier by remember { mutableStateOf("BlueDart Express") }
        var trackingNo by remember { mutableStateOf("BD" + (10000000..99999999).random()) }

        AlertDialog(
            onDismissRequest = { orderForFulfillment = null },
            title = { Text("Process Dropship Supplier Order") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Order: #${ord.orderNumber} (₹${ord.totalAmount.toInt()})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = supplierName, onValueChange = { supplierName = it }, label = { Text("Supplier Name") })
                    OutlinedTextField(value = supplierOrderNo, onValueChange = { supplierOrderNo = it }, label = { Text("Supplier PO / Order ID") })
                    OutlinedTextField(value = courier, onValueChange = { courier = it }, label = { Text("Courier Service") })
                    OutlinedTextField(value = trackingNo, onValueChange = { trackingNo = it }, label = { Text("Tracking Number") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.processSupplierFulfillment(ord.id, supplierName, supplierOrderNo, trackingNo, courier)
                    orderForFulfillment = null
                }) {
                    Text("Confirm Fulfillment")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderForFulfillment = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MarketingScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val campaigns by viewModel.campaigns.collectAsState()
    val aiMarketingBundle by viewModel.aiMarketingBundle.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var productNameInput by remember { mutableStateOf("AeroRing Pro Smart Ring") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_marketing_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "Marketing & AI Social Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Generate high-converting ad copy and social posts using Gemini 3.5 Flash, and track affiliate / social campaign clicks.", style = MaterialTheme.typography.bodySmall, color = Slate600)
        }

        // AI Marketing Generator Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "AI Viral Copy Generator", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = productNameInput,
                        onValueChange = { productNameInput = it },
                        label = { Text("Target Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.generateMarketingCopy(productNameInput, 2499.0)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Violet500),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Multichannel Copy")
                        }
                    }

                    if (aiMarketingBundle != null) {
                        val bundle = aiMarketingBundle!!
                        CopySnippetCard(
                            platform = "Instagram Caption",
                            copy = bundle.instagramCaption,
                            onCopy = { copyToClipboard(context, bundle.instagramCaption) }
                        )
                        CopySnippetCard(
                            platform = "WhatsApp Broadcast",
                            copy = bundle.whatsappMessage,
                            onCopy = { copyToClipboard(context, bundle.whatsappMessage) }
                        )
                        CopySnippetCard(
                            platform = "Reels Script",
                            copy = bundle.reelsScript,
                            onCopy = { copyToClipboard(context, bundle.reelsScript) }
                        )
                    }
                }
            }
        }

        item {
            Text(text = "Active Tracking Campaigns (${campaigns.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(campaigns) { camp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = camp.name, fontWeight = FontWeight.Bold)
                        StatusBadge(status = camp.platform)
                    }
                    Text(text = "Tracking Code: ${camp.trackingCode}", fontSize = 11.sp, color = Slate600)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Clicks: ${camp.clicksCount}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Orders: ${camp.ordersCount}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Revenue: ₹${camp.revenueGenerated.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                    }
                }
            }
        }
    }
}

@Composable
fun CopySnippetCard(
    platform: String,
    copy: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate100),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = platform, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Indigo700)
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Slate700, modifier = Modifier.size(16.dp))
                }
            }
            Text(text = copy, fontSize = 11.sp, color = Slate800, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("AI Marketing Copy", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}

@Composable
fun AutomationScreen(viewModel: MainViewModel) {
    val rules by viewModel.automationRules.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_automation_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "Dropshipping Automation Engine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Configure rules to auto-monitor supplier pricing spikes, stock changes, and automated customer notifications.", style = MaterialTheme.typography.bodySmall, color = Slate600)
        }

        items(rules) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = rule.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { viewModel.toggleAutomationRule(rule.id, it) }
                        )
                    }
                    Text(text = "Trigger: ${rule.triggerDesc}", fontSize = 12.sp, color = Slate600)
                    Text(text = "Action: ${rule.actionDesc}", fontSize = 12.sp, color = Slate700)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Last executed: ${rule.lastRun ?: "Never"}", fontSize = 11.sp, color = Slate400)
                        Button(
                            onClick = { viewModel.runAutomationRule(rule) },
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Run Now", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitScreen(viewModel: MainViewModel) {
    val profitSummary by viewModel.profitSummary.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_profit_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "Financial & Profit Statements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Real-time calculation of Dropshipping COGS, Shipping costs, Gateway fees, and Net profit margin.", style = MaterialTheme.typography.bodySmall, color = Slate600)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Profit & Loss Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Divider(color = Slate200)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Revenue (Orders):", color = Slate700)
                        Text("₹${profitSummary.totalRevenue.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Supplier COGS (Product Costs):", color = Slate700)
                        Text("-₹${profitSummary.totalProductCost.toInt()}", color = Rose500, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Courier & Express Shipping:", color = Slate700)
                        Text("-₹${profitSummary.totalShipping.toInt()}", color = Amber700, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Razorpay Gateway Fees (2%):", color = Slate700)
                        Text("-₹${profitSummary.totalPaymentFees.toInt()}", color = Slate600)
                    }
                    Divider(color = Slate200)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Retained Profit:", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            "₹${profitSummary.netProfit.toInt()} (${profitSummary.averageMarginPercent.toInt()}%)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Emerald700
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val supabaseStatus by viewModel.supabaseStatus.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "Commerce Store Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            SupabaseStatusCard(status = supabaseStatus, onRefresh = { viewModel.testSupabase() })
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Store Configuration", fontWeight = FontWeight.Bold)
                    Text(text = "Store Name: AI Commerce Store", fontSize = 12.sp, color = Slate700)
                    Text(text = "Default Currency: INR (₹)", fontSize = 12.sp, color = Slate700)
                    Text(text = "Auto Price Parity Monitor: Enabled", fontSize = 12.sp, color = Slate700)
                    Text(text = "Auto Stock Sync: Enabled", fontSize = 12.sp, color = Slate700)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Database Schema Migration", fontWeight = FontWeight.Bold)
                    Text(
                        text = "The complete PostgreSQL schema with all 29 tables, RLS policies, foreign keys, and indexes is written at:\n'supabase/migrations/01_initial_schema.sql'.",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
            }
        }
    }
}
