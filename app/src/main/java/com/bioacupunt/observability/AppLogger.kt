package com.bioacupunt.observability

object AppLogger {

    private const val PREFIX_MAX = 23
    private const val MAX_TAG_LEN = 23

    private fun sanitize(tag: String?): String {
        val source = tag ?: "App"
        return if (source.length > MAX_TAG_LEN) source.substring(0, MAX_TAG_LEN) else source
    }

    fun d(tag: String?, message: String, throwable: Throwable? = null) {
        val safe = truncate(message)
        if (throwable == null) {
            logLine("DEBUG", sanitize(tag), safe)
        } else {
            logLine("DEBUG", sanitize(tag), safe + "\n" + truncate(throwable.toString()))
        }
    }

    fun e(tag: String?, message: String, throwable: Throwable? = null) {
        val safe = truncate(message)
        if (throwable == null) {
            logLine("ERROR", sanitize(tag), safe)
        } else {
            logLine("ERROR", sanitize(tag), safe + "\n" + truncate(throwable.toString()))
        }
    }

    fun i(tag: String?, message: String, throwable: Throwable? = null) {
        val safe = truncate(message)
        if (throwable == null) {
            logLine("INFO", sanitize(tag), safe)
        } else {
            logLine("INFO", sanitize(tag), safe + "\n" + truncate(throwable.toString()))
        }
    }

    fun w(tag: String?, message: String, throwable: Throwable? = null) {
        val safe = truncate(message)
        if (throwable == null) {
            logLine("WARN", sanitize(tag), safe)
        } else {
            logLine("WARN", sanitize(tag), safe + "\n" + truncate(throwable.toString()))
        }
    }

    private fun truncate(text: String): String {
        val max = 1200
        return if (text.length <= max) text else text.substring(0, max) + "..."
    }

    private fun logLine(level: String, tag: String, message: String) {
        val prefix = "%-5s %-${PREFIX_MAX}s: %s"
        android.util.Log.println(levelToPriority(level), tag, message)
    }

    private fun levelToPriority(level: String): Int = when (level) {
        "DEBUG" -> android.util.Log.DEBUG
        "INFO" -> android.util.Log.INFO
        "WARN" -> android.util.Log.WARN
        "ERROR" -> android.util.Log.ERROR
        else -> android.util.Log.VERBOSE
    }
}
