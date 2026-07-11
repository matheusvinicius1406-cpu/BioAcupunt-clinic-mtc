package com.bioacupunt.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.data.remote.PatientCreateRequest
import com.bioacupunt.observability.AppLogger
import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.sync.data.local.SyncQueueDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Background sync worker. Processes the offline queue safely:
 * - Limits retries to avoid infinite loops / battery drain
 * - Distinguishes network errors (retry) from server errors (fail permanently)
 * - Never crashes the app — all exceptions are caught and logged
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val dao: SyncQueueDao,
    private val api: PatientApi
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val MAX_RETRIES = 5
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pending = dao.getPending()
            if (pending.isEmpty()) return@withContext Result.success()

            var hasTransientFailure = false

            pending.forEach { item ->
                if (item.retryCount >= MAX_RETRIES) {
                    dao.updateStatus(item.id, "FAILED_PERMANENT")
                    return@forEach
                }

                try {
                    dao.updateStatus(item.id, "SYNCING")

                    when (item.entityType) {
                        "Patient" -> syncPatient(item.operation, item.payloadJson)
                    }

                    dao.updateStatus(item.id, "SYNCED")
                } catch (e: IOException) {
                    // Network error — retry later
                    AppLogger.w("SyncWorker", "Network error syncing ${item.entityType}#${item.entityId}", e)
                    dao.incrementRetry(item.id, e.message)
                    dao.updateStatus(item.id, "PENDING")
                    hasTransientFailure = true
                } catch (e: Exception) {
                    // Server / data error — log but don't block other items
                    AppLogger.e("SyncWorker", "Sync failed for ${item.entityType}#${item.entityId}", e)
                    dao.incrementRetry(item.id, e.message)
                    dao.updateStatus(item.id, "ERROR")
                }
            }

            if (hasTransientFailure) Result.retry() else Result.success()
        } catch (e: Exception) {
            AppLogger.e("SyncWorker", "Fatal sync worker error", e)
            Result.failure()
        }
    }

    private suspend fun syncPatient(operation: String, payloadJson: String) {
        when (operation) {
            "CREATE" -> {
                val patient = Json.decodeFromString<Patient>(payloadJson)
                api.create(PatientCreateRequest(name = patient.name, document = patient.document))
            }
        }
    }
}
