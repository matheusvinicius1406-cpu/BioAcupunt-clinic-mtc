package com.bioacupunt.security

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthThrottle(private val context: Context) {
    companion object {
        private const val PREFS = "bio_auth_throttle"
        private const val KEY_FAILS = "fail_streak"
        private const val KEY_LOCK_UNTIL = "lock_until_epoch_ms"
        private const val KEY_LAST_FAIL = "last_fail_epoch_ms"
        private const val MAX_FAILS = 5
        private const val BASE_DELAY_MS = 1_500L
        private const val MAX_DELAY_MS = 30_000L
        private const val COOL_MS = 120_000L
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(Status.Allowed)
    val status: StateFlow<Status> = _status.asStateFlow()

    data class Status(
        val code: Code,
        val remainingFails: Int = 0,
        val delayMillis: Long = 0L,
        val message: String? = null
    ) {
        enum class Code { Allowed, Throttled, Locked }
    }

    init {
        recomputeLockAfterReboot()
    }

    fun evaluateUserAction(): Status {
        recomputeLockAfterReboot()
        return status.value
    }

    fun blockOrAllow(): Boolean = evaluateUserAction().code == Status.Code.Allowed

    private fun recomputeLockAfterReboot() {
        val until = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        val now = System.currentTimeMillis()
        when {
            until > now -> _status.value = Status(Status.Code.Locked, message = "Bloqueado por tentativas excessivas.")
            until > 0L && until <= now -> clearLock()
            else -> Unit
        }
    }

    private fun clearLock() {
        prefs.edit()
            .remove(KEY_FAILS)
            .remove(KEY_LAST_FAIL)
            .remove(KEY_LOCK_UNTIL)
            .apply()
        _status.value = Status.Allowed
    }

    fun recordFailure(): Status {
        if (status.value.code == Status.Code.Locked) return status.value
        val fails = prefs.getInt(KEY_FAILS, 0) + 1
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_FAILS, fails)
            .putLong(KEY_LAST_FAIL, now)
            .apply()
        if (fails >= MAX_FAILS) {
            val lockUntil = now + COOL_MS
            prefs.edit().putLong(KEY_LOCK_UNTIL, lockUntil).apply()
            _status.value = Status(Status.Code.Locked, message = "Muitas tentativas. Aguarde antes de tentar.")
            return _status.value
        }
        val delay = min(BASE_DELAY_MS * (1L shl (fails - 1)), MAX_DELAY_MS)
        _status.value = Status(Status.Code.Throttled, remainingFails = MAX_FAILS - fails, delayMillis = delay)
        return _status.value
    }

    fun recordSuccess() = clearLock()

    fun delayForCurrent(): Long {
        val s = status.value
        return if (s.code == Status.Code.Throttled) s.delayMillis else 0L
    }

    fun failureCount(): Int = prefs.getInt(KEY_FAILS, 0)
}
