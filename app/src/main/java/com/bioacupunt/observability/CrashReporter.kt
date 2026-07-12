package com.bioacupunt.observability

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Persists the stack trace of any uncaught exception to a file, so that after
 * the app is reopened the exact error can be shown on screen (see
 * CrashReportScreen) instead of the process just disappearing. This turns
 * "the app closes by itself" into a readable, shareable crash report.
 */
object CrashReporter {

    private const val CRASH_FILE = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash(appContext, thread, throwable) }
            // Still let the platform handle it (show ANR/crash, terminate).
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        PrintWriter(sw).use { throwable.printStackTrace(it) }
        val body = buildString {
            appendLine("BioAcupunt — relatório de erro")
            appendLine("Quando: ${java.util.Date()}")
            appendLine("Thread: ${thread.name}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Aparelho: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine()
            append(sw.toString())
        }
        File(context.filesDir, CRASH_FILE).writeText(body)
    }

    /** Returns the last crash report (and deletes it), or null if there is none. */
    fun consumeLastCrash(context: Context): String? {
        val file = File(context.applicationContext.filesDir, CRASH_FILE)
        if (!file.exists()) return null
        return runCatching {
            val text = file.readText()
            file.delete()
            text
        }.getOrNull()
    }
}
