package com.bioacupunt.cache

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Unified cache manager: LRU in-memory + disk fallback.
 * Prevents memory bloat and avoids redundant AI calls.
 */
class AppCacheManager(context: Context) {

    // In-memory LRU: max 4MB
    private val memCache = LruCache<String, String>(4 * 1024 * 1024) {
        _, value -> value.length
    }

    private val cacheDir = File(context.cacheDir, "bioacupunt_cache").also { it.mkdirs() }
    private val aiCacheDir = File(cacheDir, "ai_responses").also { it.mkdirs() }
    private val imageCacheDir = File(cacheDir, "images").also { it.mkdirs() }

    // ── Memory cache ───────────────────────────────────────
    fun get(key: String): String? = memCache.get(key)

    fun put(key: String, value: String) {
        memCache.put(key, value)
    }

    fun remove(key: String) = memCache.remove(key)

    fun clearMemory() = memCache.evictAll()

    // ── Disk cache ─────────────────────────────────────────
    suspend fun getDisk(key: String): String? = withContext(Dispatchers.IO) {
        val file = File(aiCacheDir, sanitizeKey(key))
        if (file.exists() && !isStale(file)) file.readText() else null
    }

    suspend fun putDisk(key: String, value: String) = withContext(Dispatchers.IO) {
        File(aiCacheDir, sanitizeKey(key)).writeText(value)
    }

    suspend fun clearDisk() = withContext(Dispatchers.IO) {
        aiCacheDir.listFiles()?.forEach { it.delete() }
        imageCacheDir.listFiles()?.forEach { it.delete() }
    }

    // ── Stats ──────────────────────────────────────────────
    fun memoryUsageKb(): Int = memCache.size() / 1024
    fun diskUsageKb(): Long = cacheDir.walkTopDown().sumOf { it.length() } / 1024

    // ── Helpers ────────────────────────────────────────────
    private fun sanitizeKey(key: String) =
        key.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(200)

    private fun isStale(file: File, maxAgeMs: Long = 24 * 60 * 60 * 1000L): Boolean =
        System.currentTimeMillis() - file.lastModified() > maxAgeMs

    companion object {
        @Volatile private var instance: AppCacheManager? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: AppCacheManager(context.applicationContext).also { instance = it }
        }
    }
}
