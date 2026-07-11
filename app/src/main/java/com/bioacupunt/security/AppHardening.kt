package com.bioacupunt.security

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import androidx.biometric.BiometricManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppHardening {

    enum class TamperRisk(val score: Int) { None(0), Low(1), Medium(2), High(3) }

    data class HardeningReport(
        val risk: TamperRisk,
        val reasons: List<String>,
        val securityLevel: Int = 0
    ) {
        val isSecure: Boolean
            get() = reasons.isEmpty()
    }

    private fun Context.isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/tmp/kingroot"
        )
        val hasProp = try {
            Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MODEL.contains("VirtualBox")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || Settings.Secure.getInt(contentResolver, Settings.Secure.ADB_ENABLED, 0) == 1
        } catch (e: Exception) { false }
        val hasBinary = paths.any { java.io.File(it).exists() }
        val hasRuntimeRoot: Boolean = try {
            Runtime.getRuntime().exec("su").waitFor() == 0
        } catch (e: Exception) { false }
        return hasProp || hasBinary || hasRuntimeRoot
    }

    fun isDebugDebuggable(context: Context): Boolean = context.isDebuggable()

    private fun Context.isDebuggable(): Boolean {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            (info.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun Context.hasHooks(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (e: ClassNotFoundException) {
            try {
                Class.forName("com.coreedit.XposedBridge")
                true
            } catch (e: ClassNotFoundException) {
                try {
                    Debug.isDebuggerConnected() || Debug.waitingForDebugger()
                } catch (e: Throwable) {
                    false
                }
            }
        }
    }

    suspend fun evaluate(context: Context): HardeningReport {
        return withContext(Dispatchers.Default) {
            val reasons = mutableListOf<String>()
            var worst = TamperRisk.None
            if (context.isDebuggable()) {
                reasons += "App está em modo debug"
                worst = TamperRisk.High
            }
            if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
                reasons += "Depurador vinculado ao processo"
                worst = TamperRisk.High
            }
            if (context.hasHooks()) {
                reasons += "Possível hooking/Frida/Xposed"
                worst = TamperRisk.High
            }
            if (context.isRooted()) {
                reasons += "Dispositivo com root/emulador não confiável"
                worst = TamperRisk.Medium
            }
            val biometricOk = runCatching {
                val bm = BiometricManager.from(context)
                bm.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
            }.getOrDefault(false)
            val securityLevel = when {
                !biometricOk -> 0
                worst == TamperRisk.High -> 2
                worst == TamperRisk.Medium -> 3
                else -> 4
            }
            HardeningReport(risk = worst, reasons = reasons, securityLevel = securityLevel)
        }
    }
}
