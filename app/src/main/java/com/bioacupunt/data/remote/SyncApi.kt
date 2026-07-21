package com.bioacupunt.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Delta sync wire types. These mirror `backend/app/schemas/sync.py` exactly —
 * if you change one side, change the other.
 *
 * Scope: patients, appointments and transactions. Clinical records do not sync.
 */

@JsonClass(generateAdapter = true)
data class SyncChangeDto(
    @Json(name = "entity_type") val entityType: String,
    val op: String,
    @Json(name = "client_id") val clientId: String,
    @Json(name = "server_id") val serverId: Long? = null,
    /** Null means "this record is new". Otherwise: the revision this edit was based on. */
    @Json(name = "base_rev") val baseRev: Long? = null,
    val payload: Map<String, Any?> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class SyncPushRequestDto(val changes: List<SyncChangeDto>)

@JsonClass(generateAdapter = true)
data class SyncChangeResultDto(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "entity_type") val entityType: String,
    /** "applied" | "conflict" | "rejected" */
    val status: String,
    @Json(name = "server_id") val serverId: Long? = null,
    val rev: Long? = null,
    /** Present on "conflict": the server's current version of the record. */
    @Json(name = "server_payload") val serverPayload: Map<String, Any?>? = null,
    val reason: String? = null,
)

@JsonClass(generateAdapter = true)
data class SyncPushResponseDto(
    val results: List<SyncChangeResultDto>,
    @Json(name = "server_rev") val serverRev: Long,
)

@JsonClass(generateAdapter = true)
data class SyncRecordDto(
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "server_id") val serverId: Long,
    @Json(name = "client_id") val clientId: String? = null,
    val rev: Long,
    val deleted: Boolean = false,
    val payload: Map<String, Any?> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class SyncPullResponseDto(
    val records: List<SyncRecordDto>,
    @Json(name = "server_rev") val serverRev: Long,
    @Json(name = "has_more") val hasMore: Boolean = false,
)

interface SyncApi {
    @POST("/api/v1/sync/push")
    suspend fun push(@Body request: SyncPushRequestDto): SyncPushResponseDto

    @GET("/api/v1/sync/pull")
    suspend fun pull(
        @Query("since") since: Long,
        @Query("limit") limit: Int = 200,
    ): SyncPullResponseDto
}
