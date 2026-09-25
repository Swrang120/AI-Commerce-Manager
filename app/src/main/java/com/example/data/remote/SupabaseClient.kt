package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SupabaseClient(context: Context? = null) {
    private val tag = "SupabaseClient"
    val supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { "https://widwrwsjiqbkqpzmuiwe.supabase.co" }
    val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "sb_publishable_YNkkAqGbQOImfGN9O3kzzw_i37CDrWY" }

    private val prefs = context?.getSharedPreferences("nexus_supabase_session", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    var authToken: String? = prefs?.getString("access_token", null)
    var refreshToken: String? = prefs?.getString("refresh_token", null)
    var currentUserId: String? = prefs?.getString("user_id", null)
    var currentUserEmail: String? = prefs?.getString("user_email", null)
    var currentProfile: UserProfile? = null

    init {
        val cachedRole = prefs?.getString("user_role", null)
        val cachedName = prefs?.getString("user_name", null)
        if (currentUserId != null && currentUserEmail != null) {
            currentProfile = UserProfile(
                id = currentUserId!!,
                email = currentUserEmail!!,
                fullName = cachedName ?: currentUserEmail!!.substringBefore('@'),
                role = cachedRole ?: "customer"
            )
        }
    }

    private fun saveSession(token: String, refresh: String?, userId: String, email: String, profile: UserProfile?) {
        authToken = token
        refreshToken = refresh
        currentUserId = userId
        currentUserEmail = email
        currentProfile = profile
        prefs?.edit()?.apply {
            putString("access_token", token)
            putString("refresh_token", refresh)
            putString("user_id", userId)
            putString("user_email", email)
            putString("user_role", profile?.role ?: "customer")
            putString("user_name", profile?.fullName ?: "")
            apply()
        }
    }

    fun clearSession() {
        authToken = null
        refreshToken = null
        currentUserId = null
        currentUserEmail = null
        currentProfile = null
        prefs?.edit()?.clear()?.apply()
    }

    private fun getAuthHeader(): String {
        return "Bearer ${authToken ?: supabaseAnonKey}"
    }

    data class SupabaseStatus(
        val isConnected: Boolean,
        val message: String,
        val tableStatus: String
    )

    suspend fun checkConnection(): SupabaseStatus = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            return@withContext SupabaseStatus(
                isConnected = false,
                message = "Supabase configuration is missing in BuildConfig",
                tableStatus = "Not Configured"
            )
        }
        try {
            val authReq = Request.Builder()
                .url("$supabaseUrl/auth/v1/settings")
                .addHeader("apikey", supabaseAnonKey)
                .get()
                .build()

            val authResp = okHttpClient.newCall(authReq).execute()
            val authOk = authResp.isSuccessful
            authResp.close()

            val restReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/products?select=count")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val restResp = okHttpClient.newCall(restReq).execute()
            val restCode = restResp.code
            val restBody = restResp.body?.string() ?: ""

            val tableStatus = if (restCode in 200..299) {
                "Cloud Schema Active"
            } else if (restBody.contains("PGRST205") || restCode == 404) {
                "Authoritative Schema Ready (supabase/migrations/01_initial_schema.sql)"
            } else {
                "HTTP $restCode: ${restBody.take(60)}"
            }

            SupabaseStatus(
                isConnected = authOk || restCode in 200..499,
                message = if (authOk) "Connected to Supabase Project: widwrwsjiqbkqpzmuiwe" else "Connection ready",
                tableStatus = tableStatus
            )
        } catch (e: Exception) {
            Log.e(tag, "Check connection error", e)
            SupabaseStatus(
                isConnected = false,
                message = "Network error: ${e.localizedMessage}",
                tableStatus = "Offline"
            )
        }
    }

    // ==========================================
    // SUPABASE AUTHENTICATION
    // ==========================================

    suspend fun signIn(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass.trim())
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseAnonKey)
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respBody = resp.body?.string() ?: ""
            if (resp.isSuccessful) {
                val json = JSONObject(respBody)
                val token = json.getString("access_token")
                val refresh = if (json.has("refresh_token") && !json.isNull("refresh_token")) json.getString("refresh_token") else null
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val userEmail = userObj.getString("email")

                authToken = token
                // Load user profile from profiles table
                val profile = fetchProfile(userId, userEmail, token)
                saveSession(token, refresh, userId, userEmail, profile)
                Result.success(profile)
            } else {
                val errorMsg = try {
                    JSONObject(respBody).optString("error_description", respBody)
                } catch (e: Exception) {
                    respBody
                }
                Result.failure(Exception("Supabase Auth Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String, fullName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass.trim())
                put("data", JSONObject().apply {
                    put("full_name", fullName.trim())
                })
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", supabaseAnonKey)
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respBody = resp.body?.string() ?: ""
            if (resp.isSuccessful) {
                val json = JSONObject(respBody)
                val token = if (json.has("access_token") && !json.isNull("access_token")) json.getString("access_token") else null
                val refresh = if (json.has("refresh_token") && !json.isNull("refresh_token")) json.getString("refresh_token") else null
                val userObj = json.optJSONObject("user")
                if (token != null && userObj != null) {
                    val userId = userObj.getString("id")
                    val userEmail = userObj.getString("email")
                    authToken = token
                    val profile = fetchProfile(userId, userEmail, token)
                    saveSession(token, refresh, userId, userEmail, profile)
                }
                Result.success("Registration successful.")
            } else {
                val errorMsg = try {
                    JSONObject(respBody).optString("msg", respBody)
                } catch (e: Exception) {
                    respBody
                }
                Result.failure(Exception("Supabase Sign Up Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
        val refresh = refreshToken ?: return@withContext false
        try {
            val payload = JSONObject().apply {
                put("refresh_token", refresh)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", supabaseAnonKey)
                .post(body)
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "")
                val newToken = json.getString("access_token")
                val newRefresh = json.optString("refresh_token", refresh)
                authToken = newToken
                refreshToken = newRefresh
                prefs?.edit()?.putString("access_token", newToken)?.putString("refresh_token", newRefresh)?.apply()
                true
            } else {
                clearSession()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchProfile(userId: String, userEmail: String, token: String? = null): UserProfile = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=*")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${token ?: authToken ?: supabaseAnonKey}")
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val arr = JSONArray(json)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    return@withContext UserProfile(
                        id = obj.getString("id"),
                        email = obj.optString("email", userEmail),
                        fullName = obj.optString("full_name", userEmail.substringBefore('@')),
                        role = obj.optString("role", "customer"),
                        avatarUrl = if (obj.has("avatar_url") && !obj.isNull("avatar_url")) obj.getString("avatar_url") else null,
                        phone = if (obj.has("phone") && !obj.isNull("phone")) obj.getString("phone") else null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Fetch profile error", e)
        }
        UserProfile(
            id = userId,
            email = userEmail,
            fullName = userEmail.substringBefore('@'),
            role = "customer"
        )
    }

    // ==========================================
    // PRODUCTS REMOTE DATA SOURCE
    // ==========================================

    suspend fun fetchProducts(forAdmin: Boolean = false): List<Product>? = withContext(Dispatchers.IO) {
        try {
            val filter = if (forAdmin) "status=neq.archived" else "status=eq.active"
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/products?select=*&$filter&order=created_at.desc")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val type = Types.newParameterizedType(List::class.java, Product::class.java)
                val adapter = moshi.adapter<List<Product>>(type)
                adapter.fromJson(json)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Fetch products error", e)
            null
        }
    }

    suspend fun insertProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(Product::class.java)
            val json = adapter.toJson(product)
            val body = json.toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/products")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            Log.e(tag, "Insert product error", e)
            false
        }
    }

    // ==========================================
    // ORDERS & PAYMENTS REMOTE DATA SOURCE
    // ==========================================

    suspend fun fetchOrders(customerId: String? = null): List<Order>? = withContext(Dispatchers.IO) {
        try {
            val filter = if (customerId != null) "&customer_id=eq.$customerId" else ""
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/orders?select=*$filter&order=created_at.desc")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val type = Types.newParameterizedType(List::class.java, Order::class.java)
                val adapter = moshi.adapter<List<Order>>(type)
                adapter.fromJson(json)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Fetch orders error", e)
            null
        }
    }

    suspend fun createOrder(order: Order): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(Order::class.java)
            val json = adapter.toJson(order)
            val body = json.toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/orders")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()

            // Also insert order items
            if (ok && order.items.isNotEmpty()) {
                insertOrderItems(order.items, order.id)
            }
            ok
        } catch (e: Exception) {
            Log.e(tag, "Create order error", e)
            false
        }
    }

    private suspend fun insertOrderItems(items: List<OrderItem>, orderId: String) = withContext(Dispatchers.IO) {
        try {
            val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
            val adapter = moshi.adapter<List<OrderItem>>(type)
            val updatedItems = items.map { it.copy(orderId = orderId) }
            val json = adapter.toJson(updatedItems)
            val body = json.toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/order_items")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            okHttpClient.newCall(req).execute().close()
        } catch (e: Exception) {
            Log.e(tag, "Insert order items error", e)
        }
    }

    suspend fun recordPayment(payment: PaymentRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(PaymentRecord::class.java)
            val json = adapter.toJson(payment)
            val body = json.toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/payments")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            Log.e(tag, "Record payment error", e)
            false
        }
    }

    // ==========================================
    // OTHER COLLECTIONS: CATEGORIES, COUPONS, RESEARCH
    // ==========================================

    suspend fun fetchCategories(): List<Category>? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/categories?select=*&order=name.asc")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val type = Types.newParameterizedType(List::class.java, Category::class.java)
                val adapter = moshi.adapter<List<Category>>(type)
                adapter.fromJson(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchCoupons(): List<Coupon>? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/coupons?is_active=eq.true")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val type = Types.newParameterizedType(List::class.java, Coupon::class.java)
                val adapter = moshi.adapter<List<Coupon>>(type)
                adapter.fromJson(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchSuppliers(): List<Supplier>? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/suppliers?select=*&order=name.asc")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val type = Types.newParameterizedType(List::class.java, Supplier::class.java)
                val adapter = moshi.adapter<List<Supplier>>(type)
                adapter.fromJson(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertResearch(item: ProductResearch): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(ProductResearch::class.java)
            val json = adapter.toJson(item)
            val body = json.toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/product_research")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // GUEST SESSION & CART (public.carts & public.cart_items)
    // ==========================================

    val guestSessionId: String by lazy {
        val existing = prefs?.getString("guest_session_token", null)
        if (!existing.isNullOrBlank()) {
            existing
        } else {
            val gen = "guest_${UUID.randomUUID()}"
            prefs?.edit()?.putString("guest_session_token", gen)?.apply()
            gen
        }
    }

    suspend fun getOrCreateCartId(customerId: String?): String? = withContext(Dispatchers.IO) {
        try {
            val queryUrl = if (customerId != null) {
                "$supabaseUrl/rest/v1/carts?customer_id=eq.$customerId&select=id&limit=1"
            } else {
                "$supabaseUrl/rest/v1/carts?session_token=eq.$guestSessionId&select=id&limit=1"
            }

            val req = Request.Builder()
                .url(queryUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val arr = JSONArray(json)
                if (arr.length() > 0) {
                    return@withContext arr.getJSONObject(0).getString("id")
                }
            }

            // Create new cart
            val newId = UUID.randomUUID().toString()
            val payload = JSONObject().apply {
                put("id", newId)
                if (customerId != null) {
                    put("customer_id", customerId)
                } else {
                    put("session_token", guestSessionId)
                }
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val createReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/carts")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            val createResp = okHttpClient.newCall(createReq).execute()
            val ok = createResp.isSuccessful
            createResp.close()
            if (ok) newId else null
        } catch (e: Exception) {
            Log.e(tag, "Get/create cart error", e)
            null
        }
    }

    suspend fun fetchCartItemsRemote(customerId: String?): List<CartItem>? = withContext(Dispatchers.IO) {
        try {
            val cartId = getOrCreateCartId(customerId) ?: return@withContext null
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/cart_items?cart_id=eq.$cartId&select=*,products(name,selling_price,images,sku)")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val arr = JSONArray(json)
                val list = mutableListOf<CartItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val prodObj = obj.optJSONObject("products")
                    list.add(
                        CartItem(
                            productId = obj.getString("product_id"),
                            name = prodObj?.optString("name") ?: "Product",
                            price = obj.optDouble("price", prodObj?.optDouble("selling_price", 0.0) ?: 0.0),
                            quantity = obj.optInt("quantity", 1),
                            imageUrl = prodObj?.optJSONArray("images")?.optString(0),
                            sku = prodObj?.optString("sku")
                        )
                    )
                }
                list
            } else null
        } catch (e: Exception) {
            Log.e(tag, "Fetch remote cart items error", e)
            null
        }
    }

    suspend fun syncCartItemRemote(customerId: String?, productId: String, quantity: Int, price: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val cartId = getOrCreateCartId(customerId) ?: return@withContext false
            val payload = JSONObject().apply {
                put("cart_id", cartId)
                put("product_id", productId)
                put("quantity", quantity)
                put("price", price)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/cart_items")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            Log.e(tag, "Sync cart item remote error", e)
            false
        }
    }

    suspend fun removeCartItemRemote(customerId: String?, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cartId = getOrCreateCartId(customerId) ?: return@withContext false
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/cart_items?cart_id=eq.$cartId&product_id=eq.$productId")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .delete()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearCartRemote(customerId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val cartId = getOrCreateCartId(customerId) ?: return@withContext false
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/cart_items?cart_id=eq.$cartId")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .delete()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    suspend fun mergeGuestCartOnLogin(newCustomerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val guestCartReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/carts?session_token=eq.$guestSessionId&select=id")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val guestResp = okHttpClient.newCall(guestCartReq).execute()
            if (!guestResp.isSuccessful) return@withContext false
            val guestCartJson = guestResp.body?.string() ?: "[]"
            val guestArr = JSONArray(guestCartJson)
            if (guestArr.length() == 0) return@withContext true

            val guestCartId = guestArr.getJSONObject(0).getString("id")
            val userCartId = getOrCreateCartId(newCustomerId) ?: return@withContext false

            // Update items to userCartId
            val updatePayload = JSONObject().apply {
                put("cart_id", userCartId)
            }
            val updateBody = updatePayload.toString().toRequestBody("application/json".toMediaType())
            val patchReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/cart_items?cart_id=eq.$guestCartId")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .patch(updateBody)
                .build()
            okHttpClient.newCall(patchReq).execute().close()

            // Delete guest cart
            val delReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/carts?id=eq.$guestCartId")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .delete()
                .build()
            okHttpClient.newCall(delReq).execute().close()
            true
        } catch (e: Exception) {
            Log.e(tag, "Merge guest cart error", e)
            false
        }
    }

    // ==========================================
    // WISHLIST (public.wishlists)
    // ==========================================

    suspend fun fetchWishlistRemote(customerId: String): List<String>? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/wishlists?customer_id=eq.$customerId&select=product_id")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val arr = JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getJSONObject(i).getString("product_id"))
                }
                list
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addToWishlistRemote(customerId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("customer_id", customerId)
                put("product_id", productId)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/wishlists")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromWishlistRemote(customerId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/wishlists?customer_id=eq.$customerId&product_id=eq.$productId")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .delete()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // REVIEWS (public.product_reviews)
    // ==========================================

    suspend fun insertReviewRemote(review: ProductReview): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("id", review.id)
                put("product_id", review.productId)
                if (review.customerId != null) put("customer_id", review.customerId)
                if (review.orderId != null) put("order_id", review.orderId)
                put("rating", review.rating)
                put("title", review.title ?: "Customer Review")
                put("review", review.comment.ifBlank { review.review })
                put("is_verified_purchase", review.isVerifiedPurchase)
                put("is_published", review.isPublished)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/product_reviews")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            Log.e(tag, "Insert review error", e)
            false
        }
    }

    suspend fun fetchReviewsRemote(productId: String? = null): List<ProductReview>? = withContext(Dispatchers.IO) {
        try {
            val filter = if (productId != null) "?product_id=eq.$productId&order=created_at.desc" else "?order=created_at.desc"
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/product_reviews$filter")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val type = Types.newParameterizedType(List::class.java, ProductReview::class.java)
                val adapter = moshi.adapter<List<ProductReview>>(type)
                adapter.fromJson(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateReviewStatusRemote(reviewId: String, isPublished: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("is_published", isPublished)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/product_reviews?id=eq.$reviewId")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .patch(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // COUPON REMOTE VALIDATION & REDEMPTION (public.coupons)
    // ==========================================

    suspend fun validateCouponRemote(code: String, subtotal: Double): Result<Coupon> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/coupons?code=eq.${code.trim().uppercase()}&select=*")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to validate coupon from server (HTTP ${resp.code})"))
            }

            val type = Types.newParameterizedType(List::class.java, Coupon::class.java)
            val adapter = moshi.adapter<List<Coupon>>(type)
            val list = adapter.fromJson(respBody)
            val coupon = list?.firstOrNull() ?: return@withContext Result.failure(Exception("Coupon code '$code' does not exist."))

            if (!coupon.isActive) {
                return@withContext Result.failure(Exception("Coupon '$code' is currently inactive."))
            }

            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            if (!coupon.startsAt.isNullOrBlank()) {
                try {
                    val sDate = sdf.parse(coupon.startsAt.take(19))
                    if (sDate != null && now < sDate.time) {
                        return@withContext Result.failure(Exception("Coupon '$code' promotion has not started yet."))
                    }
                } catch (_: Exception) {}
            }

            if (!coupon.expiresAt.isNullOrBlank()) {
                try {
                    val eDate = sdf.parse(coupon.expiresAt.take(19))
                    if (eDate != null && now > eDate.time) {
                        return@withContext Result.failure(Exception("Coupon '$code' has expired."))
                    }
                } catch (_: Exception) {}
            }

            if (subtotal < coupon.minimumOrderAmount) {
                return@withContext Result.failure(Exception("Order subtotal (₹${subtotal.toInt()}) must be at least ₹${coupon.minimumOrderAmount.toInt()} to use this coupon."))
            }

            if (coupon.usageLimit != null && coupon.usedCount >= coupon.usageLimit) {
                return@withContext Result.failure(Exception("Coupon '$code' has reached its maximum redemption limit."))
            }

            Result.success(coupon)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun redeemCouponAtomic(couponId: String, currentUsedCount: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("used_count", currentUsedCount + 1)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            // Conditional PATCH to enforce atomic increment without race conditions
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/coupons?id=eq.$couponId&used_count=eq.$currentUsedCount")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "return=representation")
                .patch(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respBody = resp.body?.string() ?: ""
            val ok = resp.isSuccessful && respBody != "[]" && respBody.isNotBlank()
            resp.close()
            ok
        } catch (e: Exception) {
            Log.e(tag, "Redeem coupon atomic error", e)
            false
        }
    }

    // ==========================================
    // AUTOMATION RUNS (public.automation_runs)
    // ==========================================

    suspend fun recordAutomationRunRemote(run: AutomationRun): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("id", run.id)
                if (run.ruleId != null) put("rule_id", run.ruleId)
                put("status", run.status)
                put("log_output", run.logOutput.ifBlank { run.outputData ?: "" })
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/automation_runs")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // STOCK & PRICE HISTORY (public.stock_history & public.price_history)
    // ==========================================

    suspend fun recordStockHistoryRemote(productId: String, oldStock: Int, newStock: Int, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("product_id", productId)
                put("old_stock", oldStock)
                put("new_stock", newStock)
                put("change_reason", reason)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/stock_history")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recordPriceHistoryRemote(productId: String, oldPrice: Double, newPrice: Double, source: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("product_id", productId)
                put("old_price", oldPrice)
                put("new_price", newPrice)
                put("source", source)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/price_history")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // CUSTOMER ADDRESSES (public.customer_addresses)
    // ==========================================

    suspend fun fetchCustomerAddresses(customerId: String): List<CustomerAddress>? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/customer_addresses?customer_id=eq.$customerId&order=created_at.desc")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string() ?: "[]"
                val type = Types.newParameterizedType(List::class.java, CustomerAddress::class.java)
                val adapter = moshi.adapter<List<CustomerAddress>>(type)
                adapter.fromJson(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCustomerAddress(address: CustomerAddress): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(CustomerAddress::class.java)
            val body = adapter.toJson(address).toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/customer_addresses")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }
}
