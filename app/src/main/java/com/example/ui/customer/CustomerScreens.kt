package com.example.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.CustomerTab
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun StoreHomeScreen(
    viewModel: MainViewModel,
    onProductClick: (Product) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wishlistIds by viewModel.wishlistProductIds.collectAsState()
    val selectedCategorySlug by viewModel.selectedCategorySlug.collectAsState()

    val filteredProducts = remember(products, selectedCategorySlug) {
        if (selectedCategorySlug == null) products
        else {
            val cat = categories.find { it.slug == selectedCategorySlug }
            products.filter { it.categoryId == cat?.id || it.categoryName?.contains(cat?.name ?: "", ignoreCase = true) == true }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("store_home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Promo Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Indigo900, Violet700)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Surface(
                            color = Amber500,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "AI OPTIMIZED DROPSHIP STORE",
                                color = Slate900,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Next-Gen Tech &\nMinimalist Lifestyle",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Imported from verified suppliers with zero markup bloat. Free express shipping on all orders over ₹999.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Indigo50
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Code: SAVE10 for 10% OFF",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Pills
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Shop by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategorySlug == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All Drops") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategorySlug == category.slug,
                            onClick = { viewModel.selectCategory(category.slug) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Featured Products Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Catalog (${filteredProducts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verified Suppliers",
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald700,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Product Cards Grid
        item {
            if (filteredProducts.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your catalog is empty.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Check back soon for new arrivals from verified suppliers.",
                            fontSize = 13.sp,
                            color = Slate600
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunked = filteredProducts.chunked(2)
                    for (rowItems in chunked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (product in rowItems) {
                                ProductCard(
                                    product = product,
                                    isWishlisted = wishlistIds.contains(product.id),
                                    onProductClick = { onProductClick(product) },
                                    onWishlistClick = { viewModel.toggleWishlist(product.id) },
                                    onAddToCart = { viewModel.addToCart(product, 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isWishlisted: Boolean,
    onProductClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onProductClick() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Slate100)
            ) {
                val imageUrl = product.images.firstOrNull()
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Outlined.ShoppingBag, contentDescription = null, tint = Slate400, modifier = Modifier.size(36.dp))
                    }
                }

                // Wishlist Icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .clickable { onWishlistClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) Rose500 else Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Discount Pill if compareAtPrice is higher
                if (product.compareAtPrice != null && product.compareAtPrice > product.sellingPrice) {
                    val discountPercent = ((product.compareAtPrice - product.sellingPrice) / product.compareAtPrice * 100).toInt()
                    Surface(
                        color = Emerald500,
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "$discountPercent% OFF",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "₹${product.sellingPrice.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )
                    if (product.compareAtPrice != null && product.compareAtPrice > product.sellingPrice) {
                        Text(
                            text = "₹${product.compareAtPrice.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Cart", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val wishlistIds by viewModel.wishlistProductIds.collectAsState()
    val isWishlisted = wishlistIds.contains(product.id)
    var selectedQty by remember { mutableIntStateOf(1) }
    var showReviewDialog by remember { mutableStateOf(false) }

    val reviews by viewModel.reviews.collectAsState()
    val productReviews = remember(reviews, product.id) {
        reviews.filter { it.productId == product.id }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close")
                        }
                        Text(
                            text = "Product Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.toggleWishlist(product.id) }) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = if (isWishlisted) Rose500 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(text = "Total Price", style = MaterialTheme.typography.labelSmall, color = Slate600)
                            Text(
                                text = "₹${(product.sellingPrice * selectedQty).toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Indigo600
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.addToCart(product, selectedQty)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                        ) {
                            Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Cart ($selectedQty)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Main Image
                item {
                    val imageUrl = product.images.firstOrNull()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Slate100), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Outlined.ShoppingBag, contentDescription = null, tint = Slate400, modifier = Modifier.size(54.dp))
                            }
                        }
                    }
                }

                // Title and SKU
                item {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(status = product.status)
                        Text(
                            text = "SKU: ${product.sku ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                        Text(
                            text = "• In Stock: ${product.stockQuantity} units",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald700,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Price Row
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "₹${product.sellingPrice.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Indigo600
                        )
                        if (product.compareAtPrice != null) {
                            Text(
                                text = "₹${product.compareAtPrice.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = TextDecoration.LineThrough,
                                color = Slate400
                            )
                        }
                    }
                }

                // Quantity Selector
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Select Quantity", fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (selectedQty > 1) selectedQty-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = selectedQty.toString(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (selectedQty < product.stockQuantity) selectedQty++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }

                // Highlights / Description
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Description & Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description.ifBlank { product.shortDescription ?: "Premium direct-to-consumer item." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate700,
                        lineHeight = 22.sp
                    )
                }

                // Customer Reviews Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Reviews (${productReviews.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showReviewDialog = true }) {
                            Text("Write a Review", color = Indigo600)
                        }
                    }
                    if (productReviews.isEmpty()) {
                        Text(
                            text = "No reviews yet. Be the first verified customer to leave feedback!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            productReviews.forEach { review ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = review.authorName, fontWeight = FontWeight.Bold)
                                            Row {
                                                repeat(review.rating) {
                                                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = review.comment, style = MaterialTheme.typography.bodySmall, color = Slate700)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    if (showReviewDialog) {
        var authorName by remember { mutableStateOf("") }
        var reviewText by remember { mutableStateOf("") }
        var rating by remember { mutableIntStateOf(5) }

        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Write a Product Review") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        label = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rating: ")
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = Amber500
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("Your Review") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (authorName.isNotBlank() && reviewText.isNotBlank()) {
                            viewModel.addProductReview(product.id, authorName, rating, reviewText)
                            showReviewDialog = false
                        }
                    }
                ) {
                    Text("Submit Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CartScreen(
    viewModel: MainViewModel,
    onExploreProducts: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponMessage by viewModel.couponMessage.collectAsState()
    var couponInput by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val subtotal = cartItems.sumOf { it.price * it.quantity }
    val discount = if (appliedCoupon != null) {
        if (appliedCoupon?.discountType == "percentage") subtotal * ((appliedCoupon?.discountValue ?: 0.0) / 100.0)
        else (appliedCoupon?.discountValue ?: 0.0)
    } else 0.0
    val shipping = if (subtotal > 999 || cartItems.isEmpty()) 0.0 else 79.0
    val total = (subtotal - discount + shipping).coerceAtLeast(0.0)

    if (cartItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Slate400
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your shopping bag is empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Discover AI-curated dropship catalog items with fast delivery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onExploreProducts,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Explore Products")
                }
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Final Total (INR)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "₹${total.toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Indigo600
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showCheckoutDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("proceed_to_checkout_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proceed to Razorpay Checkout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Text(
                        text = "Shopping Bag (${cartItems.size} items)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                items(cartItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.imageUrl != null) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${item.price.toInt()} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                                Text(
                                    text = "Total: ₹${(item.price * item.quantity).toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    color = Indigo600
                                )
                            }
                            IconButton(onClick = { viewModel.removeFromCart(item.productId) }) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Remove", tint = Rose500)
                            }
                        }
                    }
                }

                // Coupon Box
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Discount Coupons", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it.uppercase() },
                                    placeholder = { Text("Enter SAVE10 / WELCOME20") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.applyCoupon(couponInput) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                                ) {
                                    Text("Apply")
                                }
                            }
                            if (couponMessage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = couponMessage ?: "",
                                    fontSize = 11.sp,
                                    color = if (appliedCoupon != null) Emerald700 else Rose500,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Price Breakdown
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Cost Breakdown", fontWeight = FontWeight.Bold)
                            Divider(color = Slate200)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bag Subtotal", color = Slate600)
                                Text("₹${subtotal.toInt()}")
                            }
                            if (discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Coupon Discount", color = Emerald700)
                                    Text("-₹${discount.toInt()}", color = Emerald700, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Shipping & Handling", color = Slate600)
                                Text(if (shipping == 0.0) "FREE" else "₹${shipping.toInt()}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            viewModel = viewModel,
            totalAmount = total,
            onDismiss = { showCheckoutDialog = false },
            onConfirmOrder = { name, email, phone, addr ->
                viewModel.placeCustomerOrder(name, email, phone, addr)
                showCheckoutDialog = false
            }
        )
    }
}

@Composable
fun CheckoutDialog(
    viewModel: MainViewModel,
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirmOrder: (name: String, email: String, phone: String, address: String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var fullName by remember(currentUser) { mutableStateOf(currentUser?.fullName ?: "") }
    var email by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = Indigo600)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checkout & Payment")
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = "Payable Amount: ₹${totalAmount.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )
                }
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your full name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("name@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Shipping Address") },
                        placeholder = { Text("House/Flat, Street, City, State, PIN") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Razorpay Payment Gateway", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Configured Razorpay page: https://razorpay.me/@santiramswargiary\nOrder will be created with status 'Pending'. Verified payment updates status to 'Paid'.",
                                fontSize = 10.sp,
                                color = Slate600
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://razorpay.me/@santiramswargiary"))
                                    context.startActivity(intent)
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Preview Razorpay Checkout Page", fontSize = 11.sp, color = Indigo600)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && email.isNotBlank() && address.isNotBlank()) {
                        onConfirmOrder(fullName, email, phone, address)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
            ) {
                Text("Confirm & Create Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun OrderTrackingScreen(viewModel: MainViewModel) {
    val orders by viewModel.orders.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Order History & Live Tracking",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your dropship items, courier dispatches, and delivery status.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Outlined.ReceiptLong, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You have no orders yet.", fontWeight = FontWeight.Bold, color = Slate800)
                            Text("Explore products in the shop to place your first order.", color = Slate600, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(orders) { order ->
                var paymentIdInput by remember { mutableStateOf("") }
                var showPaymentIdField by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Order #${order.orderNumber}", fontWeight = FontWeight.Bold)
                                Text(text = order.createdAt ?: "Recently", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            }
                            StatusBadge(status = order.status)
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = Slate200)

                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.productName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹${item.totalPrice.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Payable", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "₹${order.totalAmount.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = Indigo600
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Delivery Address:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                Text(text = order.shippingAddress, fontSize = 11.sp, color = Slate600)
                            }
                        }

                        // Real Razorpay Payment Handling
                        if (order.status == "pending" || order.paymentStatus == "pending") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Amber500.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Payment Status: Awaiting Verified Payment",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Amber900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Complete payment on the official Razorpay page, then enter your Payment ID to confirm.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://razorpay.me/@santiramswargiary"))
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Pay via Razorpay", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { showPaymentIdField = !showPaymentIdField },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Verify Payment", fontSize = 11.sp)
                                        }
                                    }

                                    if (showPaymentIdField) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = paymentIdInput,
                                                onValueChange = { paymentIdInput = it },
                                                label = { Text("Razorpay Payment ID") },
                                                placeholder = { Text("pay_XXXXXXXXXXXX") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    if (paymentIdInput.isNotBlank()) {
                                                        viewModel.verifyOrderPayment(order.id, paymentIdInput.trim())
                                                        showPaymentIdField = false
                                                    }
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                                            ) {
                                                Text("Submit")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Emerald500.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Payment Verified & Captured via Razorpay",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Emerald700
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
