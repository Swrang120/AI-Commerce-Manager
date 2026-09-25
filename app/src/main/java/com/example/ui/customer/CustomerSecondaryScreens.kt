package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.CustomerTab
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun CustomerCategoriesScreen(
    viewModel: MainViewModel,
    onCategoryClick: (Category) -> Unit
) {
    val categories by viewModel.categories.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Browse Departments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "High-performing dropship niches backed by verified supplier sources.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (categories.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No categories found in store database.", color = Slate600)
                    }
                }
            }
        } else {
            items(categories) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(cat) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Indigo50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = Indigo600,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = cat.description ?: "Verified dropshipping supplier catalog",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerSearchScreen(
    viewModel: MainViewModel,
    onProductClick: (Product) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val products by viewModel.products.collectAsState()
    val wishlistIds by viewModel.wishlistProductIds.collectAsState()

    val searchResults = remember(query, products) {
        if (query.isBlank()) emptyList()
        else products.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("customer_search_screen")
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search products, gadgets, accessories...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Type a keyword to search active dropship products", color = Slate600, fontSize = 13.sp)
                }
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching products found for '$query'", color = Slate600)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(searchResults) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProductClick(product) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = product.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(text = "₹${product.sellingPrice.toInt()}", color = Indigo600, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(onClick = { viewModel.addToCart(product, 1) }) {
                                Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add to Cart", tint = Indigo600)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerWishlistScreen(
    viewModel: MainViewModel,
    onProductClick: (Product) -> Unit
) {
    val wishlistIds by viewModel.wishlistProductIds.collectAsState()
    val products by viewModel.products.collectAsState()
    val wishlistedProducts = remember(wishlistIds, products) {
        products.filter { wishlistIds.contains(it.id) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(text = "My Wishlist (${wishlistedProducts.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (wishlistedProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No items wishlisted yet.", color = Slate600)
                    }
                }
            }
        } else {
            items(wishlistedProducts) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductClick(product) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = product.name, fontWeight = FontWeight.Bold)
                            Text(text = "₹${product.sellingPrice.toInt()}", color = Indigo600, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.addToCart(product, 1) }) {
                            Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add to Cart", tint = Indigo600)
                        }
                        IconButton(onClick = { viewModel.toggleWishlist(product.id) }) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "Remove", tint = Rose500)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerAccountScreen(
    viewModel: MainViewModel,
    onSwitchToAdmin: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var isSignUpTab by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var fullNameInput by remember { mutableStateOf("") }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var adminAccessWarning by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(text = "Customer Account & Authentication", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Authenticate with Supabase to save order history, track deliveries, and access store operations.", fontSize = 12.sp, color = Slate600)
        }

        if (currentUser == null) {
            // Unauthenticated: Show Real Supabase Auth Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TabRow(selectedTabIndex = if (isSignUpTab) 1 else 0, modifier = Modifier.fillMaxWidth()) {
                                Tab(
                                    selected = !isSignUpTab,
                                    onClick = { isSignUpTab = false; authMessage = null },
                                    text = { Text("Sign In") }
                                )
                                Tab(
                                    selected = isSignUpTab,
                                    onClick = { isSignUpTab = true; authMessage = null },
                                    text = { Text("Sign Up") }
                                )
                            }
                        }

                        if (isSignUpTab) {
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = { fullNameInput = it },
                                label = { Text("Full Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (authMessage != null) {
                            Text(
                                text = authMessage ?: "",
                                fontSize = 12.sp,
                                color = if (authMessage?.contains("Welcome", ignoreCase = true) == true || authMessage?.contains("Success", ignoreCase = true) == true) Emerald700 else Rose500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                    isAuthLoading = true
                                    if (isSignUpTab) {
                                        viewModel.signUp(emailInput, passwordInput, fullNameInput) { success, msg ->
                                            isAuthLoading = false
                                            authMessage = msg
                                        }
                                    } else {
                                        viewModel.signIn(emailInput, passwordInput) { success, msg ->
                                            isAuthLoading = false
                                            authMessage = msg
                                        }
                                    }
                                } else {
                                    authMessage = "Please enter valid email and password"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(if (isSignUpTab) "Create Account" else "Sign In")
                            }
                        }
                    }
                }
            }
        } else {
            // Authenticated Profile Card
            val profile = currentUser!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Indigo600),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.fullName.take(2).uppercase().ifBlank { "ME" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = profile.fullName.ifBlank { "User" }, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(text = profile.email, color = Slate600, fontSize = 13.sp)
                                Surface(
                                    color = if (profile.role == "admin" || profile.role == "manager") Amber500.copy(alpha = 0.15f) else Slate100,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Role: ${profile.role.uppercase()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profile.role == "admin" || profile.role == "manager") Amber900 else Slate700,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // Admin Portal Entry (Protected strictly by profile role!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = Slate800)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Admin Access Portal", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Access is restricted to authorized store staff. Authorization is strictly verified server-side.",
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    if (adminAccessWarning != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = adminAccessWarning ?: "",
                            color = Rose500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val role = currentUser?.role?.lowercase() ?: ""
                            if (role == "admin" || role == "manager") {
                                adminAccessWarning = null
                                onSwitchToAdmin()
                            } else {
                                adminAccessWarning = "Admin access required. Please sign in with an administrator account."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text("Open Admin Commerce Manager")
                    }
                }
            }
        }

        // Backend Infrastructure Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Cloud Infrastructure & Integrations", fontWeight = FontWeight.Bold)
                    Text(text = "Backend: Supabase Cloud PostgreSQL", fontSize = 12.sp, color = Slate700)
                    Text(text = "Security: PostgreSQL RLS Policies & Auth Sessions", fontSize = 12.sp, color = Slate700)
                    Text(text = "Payment Gateway: Razorpay (https://razorpay.me/@santiramswargiary)", fontSize = 12.sp, color = Slate700)
                    Text(text = "AI Engine: Google Gemini 2.5 Flash", fontSize = 12.sp, color = Slate700)
                }
            }
        }
    }
}
