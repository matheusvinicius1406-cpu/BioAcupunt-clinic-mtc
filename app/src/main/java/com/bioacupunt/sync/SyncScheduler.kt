package com.bioacupunt.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules sync work with WorkManager.
 * - Immediate sync: triggered after local writes (create/update patient)
 * - Periodic sync: runs every 15 min in background as safety net
 * Both respect battery/network constraints to avoid drain.
 */
class SyncScheduler(private val context: Context) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("immediate_sync", ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("periodic_sync", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
