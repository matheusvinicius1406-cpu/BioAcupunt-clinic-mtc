package com.bioacupunt.sync

import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.sync.data.local.SyncQueueDao
import com.bioacupunt.sync.data.local.SyncQueueEntity
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

class SyncManager(
    private val repository: SyncQueueRepository,
    private val api: PatientApi,
    private val dao: SyncQueueDao
) {
    suspend fun sync() {
        val pending = repository.enqueue(SyncQueueEntity()) ?: emptyList()
        val items = dao.pending().firstOrNull().orEmpty()

        Timber.d("SyncManager: %d pending items", items.size)

        items.forEach { item ->
            try {
                Timber.d("SyncManager: syncing %s / %s", item.entityId, item.operation)

                val response = api.syncPatient(
                    PatientApi.SyncPatientRequest(
                        entityId = item.entityId,
                        operation = item.operation,
                        payloadJson = item.payloadJson
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    dao.markSynced(item.id, System.currentTimeMillis())
                    Timber.d("SyncManager: synced %s", item.entityId)
                } else {
                    val message = response.body()?.message ?: response.message()
                    dao.markError(item.id, message)
                    Timber.w("SyncManager: failed %s - %s", item.entityId, message)
                }
            } catch (e: Exception) {
                dao.markError(item.id, e.message)
                Timber.e(e, "SyncManager: error %s", item.entityId)
            }
        }
    }
}
