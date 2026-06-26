package com.bioacupunt.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.sync.data.local.SyncQueueDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val dao: SyncQueueDao,
    private val api: PatientApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pending = dao.getPending()

        pending.forEach { item ->
            try {
                dao.updateStatus(item.id, "SYNCING")

                when (item.entityType) {
                    "Patient" -> {
                        api.syncPatient(
                            PatientApi.SyncPatientRequest(
                                entityId = item.entityId,
                                operation = item.operation,
                                payloadJson = item.payloadJson
                            )
                        )
                    }
                }

                dao.updateStatus(item.id, "SYNCED")
            } catch (e: Exception) {
                dao.incrementRetry(item.id, e.message)
                dao.updateStatus(item.id, "ERROR")
            }
        }

        Result.success()
    }
}
