package com.bioacupunt.crm.adapter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Twenty CRM API Client — real HTTP implementation.
 *
 * Communicates with Twenty's REST API (v2) over HTTPS.
 * Strategy B: Twenty runs as a separate service; BioAcupunt consumes through this adapter.
 *
 * Configuration:
 * - base URL: Configured via SecurePreferences (never hardcoded)
 * - Authentication: Bearer token (OAuth2 personal access token from Twenty settings)
 * - Timeout: 15s connect, 30s read
 * - Retry: 3 attempts with exponential backoff on 5xx/timeout
 *
 * Safety:
 * - Never logs tokens or API keys
 * - Never hardcodes secrets
 * - All network errors caught and returned as ApiResponse.error
 * - Idempotent for GET operations
 */
class TwentyApiClient(
    private val baseUrl: String,
    private val token: String,
) {

    // ═════════════════════════════════════════════════════════════════════
    // HTTP Client
    // ═════════════════════════════════════════════════════════════════════

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // We handle retry manually for exponential backoff
            .build()
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data classes
    // ═════════════════════════════════════════════════════════════════════

    data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val error: String? = null,
        val statusCode: Int = 200,
    )

    data class TwentyRecord(
        val id: String,
        val objectMetadataId: String,
        val fields: Map<String, Any?> = emptyMap(),
        val createdAt: String = "",
        val updatedAt: String = "",
    )

    data class PaginatedResponse<T>(
        val data: List<T>,
        val totalCount: Int,
        val hasNextPage: Boolean,
        val endCursor: String?,
    )

    // ═════════════════════════════════════════════════════════════════════
    // Internal HTTP methods
    // ═════════════════════════════════════════════════════════════════════

    private suspend fun executeRequest(
        method: String,
        path: String,
        body: String? = null,
        maxRetries: Int = 3,
    ): ApiResponse<JSONObject> = withContext(Dispatchers.IO) {
        val url = "${baseUrl.trimEnd('/')}$path"

        var lastError: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")

                when (method.uppercase()) {
                    "GET" -> requestBuilder.get()
                    "POST" -> {
                        val requestBody = (body ?: "{}").toRequestBody(jsonMediaType)
                        requestBuilder.post(requestBody)
                    }
                    "PATCH" -> {
                        val requestBody = (body ?: "{}").toRequestBody(jsonMediaType)
                        requestBuilder.patch(requestBody)
                    }
                    "DELETE" -> requestBuilder.delete()
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: "{}"

                when (response.code) {
                    in 200..299 -> {
                        val json = try {
                            JSONObject(responseBody)
                        } catch (e: Exception) {
                            JSONObject().apply { put("raw", responseBody) }
                        }
                        return@withContext ApiResponse(
                            success = true,
                            data = json,
                            statusCode = response.code,
                        )
                    }
                    401 -> return@withContext ApiResponse(
                        success = false,
                        error = "Unauthorized — check API token",
                        statusCode = 401,
                    )
                    403 -> return@withContext ApiResponse(
                        success = false,
                        error = "Forbidden — insufficient permissions",
                        statusCode = 403,
                    )
                    404 -> return@withContext ApiResponse(
                        success = false,
                        error = "Not found: $path",
                        statusCode = 404,
                    )
                    409 -> return@withContext ApiResponse(
                        success = false,
                        error = "Conflict — resource already exists",
                        statusCode = 409,
                    )
                    429 -> {
                        // Rate limited — retry after delay
                        if (attempt < maxRetries - 1) {
                            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 2L
                            Thread.sleep(retryAfter * 1000)
                            return@repeat // retry
                        }
                        return@withContext ApiResponse(
                            success = false,
                            error = "Rate limited",
                            statusCode = 429,
                        )
                    }
                    in 500..599 -> {
                        // Server error — retry with exponential backoff
                        if (attempt < maxRetries - 1) {
                            Thread.sleep((1000L * (1 shl attempt))) // 1s, 2s, 4s
                            return@repeat // retry
                        }
                        return@withContext ApiResponse(
                            success = false,
                            error = "Server error: ${response.code}",
                            statusCode = response.code,
                        )
                    }
                    else -> return@withContext ApiResponse(
                        success = false,
                        error = "HTTP ${response.code}: $responseBody",
                        statusCode = response.code,
                    )
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep(1000L * (1 shl attempt))
                }
            } catch (e: java.io.IOException) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep(1000L * (1 shl attempt))
                }
            }
        }

        ApiResponse(
            success = false,
            error = "Network error: ${lastError?.message ?: "unknown"}",
            statusCode = 0,
        )
    }

    private fun parseRecord(json: JSONObject): TwentyRecord {
        val fields = mutableMapOf<String, Any?>()
        val data = json.optJSONObject("data") ?: json
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key != "id" && key != "createdAt" && key != "updatedAt" && key != "objectMetadataId") {
                fields[key] = data.opt(key)
            }
        }
        return TwentyRecord(
            id = data.optString("id", ""),
            objectMetadataId = data.optString("objectMetadataId", ""),
            fields = fields,
            createdAt = data.optString("createdAt", ""),
            updatedAt = data.optString("updatedAt", ""),
        )
    }

    private fun parseRecordList(json: JSONObject): List<TwentyRecord> {
        val data = json.optJSONObject("data") ?: return emptyList()
        val edges = data.optJSONArray("edges") ?: return emptyList()
        val records = mutableListOf<TwentyRecord>()
        for (i in 0 until edges.length()) {
            val edge = edges.optJSONObject(i) ?: continue
            val node = edge.optJSONObject("node") ?: continue
            records.add(parseRecord(node))
        }
        return records
    }

    // ═════════════════════════════════════════════════════════════════════
    // Person operations
    // ═════════════════════════════════════════════════════════════════════

    suspend fun createPerson(
        firstName: String,
        lastName: String,
        email: String? = null,
        phone: String? = null,
    ): ApiResponse<TwentyRecord> {
        val fields = JSONObject().apply {
            put("firstName", firstName)
            put("lastName", lastName)
            if (!email.isNullOrBlank()) put("email", email)
            if (!phone.isNullOrBlank()) put("phone", phone)
        }
        val body = JSONObject().apply { put("data", fields) }.toString()
        val result = executeRequest("POST", "/api/v1/people", body)
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun getPerson(id: String): ApiResponse<TwentyRecord> {
        val result = executeRequest("GET", "/api/v1/people/$id")
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun updatePerson(
        id: String,
        fields: Map<String, Any?>,
    ): ApiResponse<TwentyRecord> {
        val body = JSONObject().apply { put("data", JSONObject(fields)) }.toString()
        val result = executeRequest("PATCH", "/api/v1/people/$id", body)
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun listPeople(
        page: Int = 1,
        limit: Int = 20,
        filter: Map<String, Any?>? = null,
    ): ApiResponse<PaginatedResponse<TwentyRecord>> {
        val path = "/api/v1/people?page=$page&limit=$limit"
        val result = executeRequest("GET", path)
        return if (result.success && result.data != null) {
            val records = parseRecordList(result.data)
            val totalCount = result.data.optJSONObject("data")?.optInt("totalCount", 0) ?: 0
            ApiResponse(
                success = true,
                data = PaginatedResponse(
                    data = records,
                    totalCount = totalCount,
                    hasNextPage = page * limit < totalCount,
                    endCursor = null,
                ),
                statusCode = result.statusCode,
            )
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Company operations
    // ═════════════════════════════════════════════════════════════════════

    suspend fun createCompany(
        name: String,
        domainName: String? = null,
    ): ApiResponse<TwentyRecord> {
        val fields = JSONObject().apply {
            put("name", name)
            if (!domainName.isNullOrBlank()) put("domainName", domainName)
        }
        val body = JSONObject().apply { put("data", fields) }.toString()
        val result = executeRequest("POST", "/api/v1/companies", body)
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun getCompany(id: String): ApiResponse<TwentyRecord> {
        val result = executeRequest("GET", "/api/v1/companies/$id")
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun listCompanies(
        page: Int = 1,
        limit: Int = 20,
    ): ApiResponse<PaginatedResponse<TwentyRecord>> {
        val path = "/api/v1/companies?page=$page&limit=$limit"
        val result = executeRequest("GET", path)
        return if (result.success && result.data != null) {
            val records = parseRecordList(result.data)
            val totalCount = result.data.optJSONObject("data")?.optInt("totalCount", 0) ?: 0
            ApiResponse(
                success = true,
                data = PaginatedResponse(
                    data = records,
                    totalCount = totalCount,
                    hasNextPage = page * limit < totalCount,
                    endCursor = null,
                ),
                statusCode = result.statusCode,
            )
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Opportunity operations
    // ═════════════════════════════════════════════════════════════════════

    suspend fun createOpportunity(
        name: String,
        amount: Double? = null,
        stage: String? = null,
    ): ApiResponse<TwentyRecord> {
        val fields = JSONObject().apply {
            put("name", name)
            if (amount != null) put("amount", amount)
            if (!stage.isNullOrBlank()) put("stage", stage)
        }
        val body = JSONObject().apply { put("data", fields) }.toString()
        val result = executeRequest("POST", "/api/v1/opportunities", body)
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun updateOpportunity(
        id: String,
        fields: Map<String, Any?>,
    ): ApiResponse<TwentyRecord> {
        val body = JSONObject().apply { put("data", JSONObject(fields)) }.toString()
        val result = executeRequest("PATCH", "/api/v1/opportunities/$id", body)
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Generic record operations
    // ═════════════════════════════════════════════════════════════════════

    suspend fun createRecord(
        objectName: String,
        fields: Map<String, Any?>,
    ): ApiResponse<TwentyRecord> {
        val body = JSONObject().apply { put("data", JSONObject(fields)) }.toString()
        val result = executeRequest("POST", "/api/v1/$objectName", body)
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun getRecord(
        objectName: String,
        id: String,
    ): ApiResponse<TwentyRecord> {
        val result = executeRequest("GET", "/api/v1/$objectName/$id")
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun updateRecord(
        objectName: String,
        id: String,
        fields: Map<String, Any?>,
    ): ApiResponse<TwentyRecord> {
        val body = JSONObject().apply { put("data", JSONObject(fields)) }.toString()
        val result = executeRequest("PATCH", "/api/v1/$objectName/$id", body)
        return if (result.success && result.data != null) {
            ApiResponse(success = true, data = parseRecord(result.data), statusCode = result.statusCode)
        } else {
            ApiResponse(success = false, error = result.error, statusCode = result.statusCode)
        }
    }

    suspend fun deleteRecord(
        objectName: String,
        id: String,
    ): ApiResponse<Boolean> {
        val result = executeRequest("DELETE", "/api/v1/$objectName/$id")
        return ApiResponse(
            success = result.success,
            data = result.success,
            error = result.error,
            statusCode = result.statusCode,
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // Health check
    // ═════════════════════════════════════════════════════════════════════

    suspend fun healthCheck(): ApiResponse<Boolean> {
        val result = executeRequest("GET", "/api/v1/health", maxRetries = 1)
        return ApiResponse(
            success = result.success,
            data = result.success,
            error = result.error,
            statusCode = result.statusCode,
        )
    }
}
