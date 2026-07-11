package com.bioacupunt.sync

import android.content.Context
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.bioacupunt.data.remote.AppointmentApi
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.sync.data.local.SyncQueueDao

class SyncWorkerFactory(
    private val dao: SyncQueueDao,
    private val api: PatientApi,
    private val appointmentApi: AppointmentApi
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ) = when (workerClassName) {
        SyncWorker::class.java.name -> SyncWorker(appContext, workerParameters, dao, api, appointmentApi)
        else -> null
    }
}