package com.bioacupunt.sync

import android.content.Context
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.bioacupunt.sync.data.local.SyncStateDao

/**
 * [engineProvider] and [stateDaoProvider] are resolved lazily inside
 * [createWorker], not eagerly at construction — this factory itself is built
 * synchronously during Application.onCreate() (WorkManager.Configuration.Provider
 * requires it), and eagerly resolving the Room database there would run its
 * migration file I/O on the main thread on every cold start.
 */
class SyncWorkerFactory(
    private val engineProvider: () -> SyncEngine,
    private val stateDaoProvider: () -> SyncStateDao,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ) = when (workerClassName) {
        SyncWorker::class.java.name ->
            SyncWorker(appContext, workerParameters, engineProvider(), stateDaoProvider())
        else -> null
    }
}
