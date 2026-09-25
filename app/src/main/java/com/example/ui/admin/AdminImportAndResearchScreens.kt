package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductResearch
import com.example.data.remote.ImportResult
import com.example.data.remote.NormalizedProductImport
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ProductImportScreen(viewModel: MainViewModel) {
    var inputUrl by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Auto-Detect") }

    val importResult by viewModel.importResult.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val aiOptimized by viewModel.aiOptimizedContent.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    val sources = listOf("Auto-Detect", "Amazon", "Flipkart", "Meesho", "Shopify", "Generic Provider")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_import_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "Supplier Product Importer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "Enter any supplier or marketplace URL to detect provider, inspect API/feed availability, normalize product details, and calculate dropshipping margins.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )
        }

        // Input Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Product / Feed URL") },
                        placeholder = { Text("https://supplier.com/products/item-handle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Source Provider:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            sources.take(3).forEach { src ->
                                FilterChip(
                                    selected = selectedSource == src,
                                    onClick = { selectedSource = src },
                                    label = { Text(src, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (inputUrl.isNotBlank()) {
                                viewModel.parseSupplierUrl(inputUrl, selectedSource)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Process URL & Inspect Provider", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Import Results Handling
        when (val result = importResult) {
            is ImportResult.Success -> {
                item {
                    ImportPreviewCard(
                        importData = result.product,
                        isAutoExtracted = true,
                        aiOptimized = aiOptimized,
                        isAiLoading = isAiLoading,
                        onOptimizeAi = { name, desc, price -> viewModel.optimizeProductWithAi(name, desc, price) },
                        onConfirm = { updated, price, margin -> viewModel.confirmImportProduct(updated, price, margin) },
                        onCancel = { viewModel.cancelImport() }
                    )
                }
            }
            is ImportResult.Unavailable -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Amber500.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Amber500)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Provider Notice", fontWeight = FontWeight.Bold, color = Slate900)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.reason,
                                fontSize = 12.sp,
                                color = Slate700,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = "Enter Product Information Manually",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImportPreviewCard(
                        importData = result.manualEntryTemplate,
                        isAutoExtracted = false,
                        aiOptimized = aiOptimized,
                        isAiLoading = isAiLoading,
                        onOptimizeAi = { name, desc, price -> viewModel.optimizeProductWithAi(name, desc, price) },
                        onConfirm = { updated, price, margin -> viewModel.confirmImportProduct(updated, price, margin) },
                        onCancel = { viewModel.cancelImport() }
                    )
                }
            }
            is ImportResult.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Rose500.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Rose500)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = result.message, color = Rose500, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
            null -> {}
        }
    }
}

@Composable
fun ImportPreviewCard(
    importData: NormalizedProductImport,
    isAutoExtracted: Boolean,
    aiOptimized: com.example.data.remote.GeminiService.AiProductContent?,
    isAiLoading: Boolean,
    onOptimizeAi: (String, String, Double) -> Unit,
    onConfirm: (NormalizedProductImport, Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    var editableName by remember(importData) { mutableStateOf(importData.name) }
    var editableDesc by remember(importData) { mutableStateOf(importData.description) }
    var editableCostText by remember(importData) { mutableStateOf(if (importData.supplierPrice > 0) importData.supplierPrice.toInt().toString() else "0") }
    var editableSellingText by remember(importData) { mutableStateOf(if (importData.suggestedSellingPrice > 0) importData.suggestedSellingPrice.toInt().toString() else "0") }
    var editableShippingText by remember(importData) { mutableStateOf("79") }
    var editableSku by remember(importData) { mutableStateOf(importData.sku) }

    val cost = editableCostText.toDoubleOrNull() ?: 0.0
    val selling = editableSellingText.toDoubleOrNull() ?: 0.0
    val shipping = editableShippingText.toDoubleOrNull() ?: 79.0
    val netProfit = (selling - cost - shipping).coerceAtLeast(0.0)
    val marginPercent = if (selling > 0) (netProfit / selling) * 100.0 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Product Details (Draft)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Status: Draft",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = editableName,
                onValueChange = { editableName = it },
                label = { Text("Product Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editableSku,
                onValueChange = { editableSku = it },
                label = { Text("SKU") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editableCostText,
                    onValueChange = { editableCostText = it },
                    label = { Text("Supplier Cost (₹)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = editableSellingText,
                    onValueChange = { editableSellingText = it },
                    label = { Text("Selling Price (₹)") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Margin & Profit Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Indigo50)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Est. Net Profit: ₹${netProfit.toInt()}", fontWeight = FontWeight.Bold, color = Indigo900)
                        Text("Profit Margin: ${marginPercent.toInt()}%", fontSize = 11.sp, color = Indigo700)
                    }
                    Text(
                        text = "Courier Est: ₹${shipping.toInt()}",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }

            OutlinedTextField(
                value = editableDesc,
                onValueChange = { editableDesc = it },
                label = { Text("Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // AI Enhancement Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Violet600, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gemini Content Optimization", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            text = "AI estimate (review before publishing)",
                            fontSize = 9.sp,
                            color = Slate500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onOptimizeAi(editableName, editableDesc, cost) },
                        colors = ButtonDefaults.buttonColors(containerColor = Violet600),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Generate AI SEO & Description", fontSize = 11.sp)
                        }
                    }

                    if (aiOptimized != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI Generated: ${aiOptimized.optimizedTitle}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                        TextButton(
                            onClick = {
                                editableName = aiOptimized.optimizedTitle
                                editableDesc = aiOptimized.fullDescription
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Apply AI Generated Copy to Fields", fontSize = 11.sp, color = Indigo600)
                        }
                    }
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = {
                        val updated = importData.copy(
                            name = editableName,
                            description = editableDesc,
                            supplierPrice = cost,
                            suggestedSellingPrice = selling,
                            sku = editableSku
                        )
                        onConfirm(updated, selling, marginPercent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save as Draft")
                }
            }
        }
    }
}

@Composable
fun ProductResearchScreen(viewModel: MainViewModel) {
    var searchKeyword by remember { mutableStateOf("") }
    var estimatedPriceText by remember { mutableStateOf("499") }
    val researchItems by viewModel.researchItems.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_research_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(text = "AI Product Lab & Market Research", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "Use Gemini AI to analyze market viability, demand velocity, competition density, and markup headroom before sourcing.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )
        }

        // Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        label = { Text("Product Concept / Niche") },
                        placeholder = { Text("e.g. Magnetic Wireless Car Charger") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = estimatedPriceText,
                        onValueChange = { estimatedPriceText = it },
                        label = { Text("Estimated Sourcing Cost (INR)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "AI estimate (review before publishing)",
                                fontSize = 10.sp,
                                color = Slate600,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Button(
                            onClick = {
                                val cost = estimatedPriceText.toDoubleOrNull() ?: 499.0
                                if (searchKeyword.isNotBlank()) {
                                    viewModel.evaluateProductResearch(searchKeyword, cost)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Violet600)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Opportunity")
                            }
                        }
                    }
                }
            }
        }

        // Research Candidates List
        item {
            Text(
                text = "Market Opportunities (${researchItems.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (researchItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No market research saved yet. Enter a concept above to analyze.", color = Slate600, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(researchItems) { item ->
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
                            Text(text = item.productName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Surface(
                                color = when (item.status) {
                                    "approved" -> Emerald500.copy(alpha = 0.15f)
                                    "shortlisted" -> Indigo50
                                    "rejected" -> Rose500.copy(alpha = 0.15f)
                                    else -> Slate100
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item.status.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.status) {
                                        "approved" -> Emerald700
                                        "shortlisted" -> Indigo600
                                        "rejected" -> Rose500
                                        else -> Slate700
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cost: ₹${item.sourcePrice.toInt()}", fontSize = 12.sp, color = Slate600)
                            Text("Est. Selling: ₹${item.estimatedSellingPrice.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Est. Net: ₹${item.estimatedProfit.toInt()}", fontSize = 12.sp, color = Emerald700, fontWeight = FontWeight.Bold)
                        }

                        // Opportunity Scores Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScorePill(label = "Demand", score = item.demandScore, color = Indigo600, modifier = Modifier.weight(1f))
                            ScorePill(label = "Competition", score = item.competitionScore, color = Amber500, modifier = Modifier.weight(1f))
                            ScorePill(label = "Opportunity", score = item.opportunityScore, color = Emerald500, modifier = Modifier.weight(1f))
                        }

                        if (!item.aiRecommendation.isNullOrBlank()) {
                            Text(
                                text = "Recommendation: ${item.aiRecommendation}",
                                fontSize = 11.sp,
                                color = Slate700,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScorePill(label: String, score: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, color = Slate600)
            Text(text = "$score/100", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
