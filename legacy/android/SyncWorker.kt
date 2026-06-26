package com.bioacupunt.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bioacupunt.di.AppContainer

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AppContainer.getSyncManager().scheduleSync()
        return Result.success()
    }
}
