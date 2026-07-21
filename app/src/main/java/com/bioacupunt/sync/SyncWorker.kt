package com.bioacupunt.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bioacupunt.observability.AppLogger
import com.bioacupunt.sync.data.local.SyncStateDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Runs one two-way sync pass in the background.
 *
 * What this replaced, and why it mattered: the previous worker dispatched on
 * entity type with a `when` that had no `else`, then marked the queue item
 * `SYNCED` unconditionally. Anything that was not a `Patient` — every
 * appointment, every transaction — was recorded as successfully uploaded
 * without a single byte being sent. It also only ever pushed; nothing was
 * downloaded, so a second device would never have seen anything.
 *
 * The rule now: **a record is marked synced only when the server says it
 * accepted it.** Everything else stays pending and is retried. A record that
 * still knows it is unsent can be recovered; one that falsely believes it is
 * safe cannot.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val engine: SyncEngine,
    private val stateDao: SyncStateDao,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val report = engine.syncOnce()
            AppLogger.i(
                "SyncWorker",
                "sync ok — pushed=${report.pushed} pulled=${report.pulled} " +
                    "conflicts=${report.conflicted} rejected=${report.rejected}",
            )

            // More waiting on the server than fitted in one batch: come straight
            // back rather than idling until the next periodic run.
            if (report.hasMore) Result.retry() else Result.success()
        } catch (e: IOException) {
            // Offline or the server is unreachable. Entirely expected on a phone
            // carried between consulting rooms — retry, and do not surface it as
            // an error the doctor has to think about.
            AppLogger.w("SyncWorker", "Network unavailable during sync", e)
            Result.retry()
        } catch (e: SyncEngine.UnknownEntityType) {
            // A programming error: something is queued that no writer handles.
            // Fail permanently and loudly. Retrying cannot fix it, and silently
            // skipping is precisely the bug this worker was rewritten to remove.
            AppLogger.e("SyncWorker", "Unsyncable entity type in queue", e)
            stateDao.recordError(e.message)
            Result.failure()
        } catch (e: Exception) {
            AppLogger.e("SyncWorker", "Sync failed", e)
            stateDao.recordError(e.message)
            Result.retry()
        }
    }
}
