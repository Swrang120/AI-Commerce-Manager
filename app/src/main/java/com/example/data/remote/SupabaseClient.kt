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
}
