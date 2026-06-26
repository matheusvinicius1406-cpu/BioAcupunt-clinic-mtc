package com.bioacupunt.sync

import android.content.Context
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.sync.data.local.SyncQueueDao
import javax.inject.Inject

class SyncWorkerFactory @Inject constructor(
    private val dao: SyncQueueDao,
    private val api: PatientApi
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ) = when (workerClassName) {
        SyncWorker::class.java.name -> SyncWorker(appContext, workerParameters, dao, api)
        else -> null
    }
}
