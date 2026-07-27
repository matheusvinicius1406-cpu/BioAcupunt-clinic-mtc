package com.bioacupunt

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.bioacupunt.di.AppContainer
import com.bioacupunt.observability.CrashReporter
import com.bioacupunt.ui.CrashReportScreen
import com.bioacupunt.ui.lock.BiometricLockScreen
import com.bioacupunt.ui.navigation.BioAcupuntNavHost
import com.bioacupunt.ui.theme.BioAcupuntTheme

/**
 * Estado global do auto-bloqueio por inatividade.
 *
 * `locked = true` significa: o app ficou tempo demais em segundo plano (mais do
 * que `SecurePreferences.autoLockMinutes`) e o conteúdo clínico NÃO pode
 * aparecer até nova autenticação. A `MainActivity` sobrepõe a tela de
 * desbloqueio sobre TODO o conteúdo enquanto isso for verdade — o NavHost segue
 * composto por baixo (preserva a pilha), mas coberto por uma superfície opaca,
 * então nenhum prontuário fica visível.
 */
object AppLock {
    val locked = mutableStateOf(false)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bootstrap failures must become a *screen*, not a silent death.
        // Anything here can legitimately fail on a real device — a Room migration,
        // a corrupted Keystore behind SecurePreferences, a full disk. Previously
        // any such failure killed the app before the first frame with no trace,
        // which is exactly how the launch crash stayed invisible for so long.
        val bootFailure = runCatching {
            AppContainer.init(applicationContext)
            AppContainer.seedDemoDataIfNeeded()
            AppContainer.seedPharmaCatalogIfNeeded()
            com.bioacupunt.ui.theme.ThemeController.load(AppContainer.securePreferences)
            // Cold start: o portão de auto-bloqueio NÃO se aplica — a tela de
            // Login (startDestination) já barra o acesso. Zerar o carimbo evita
            // um "duplo cadeado" espúrio (Login + overlay) na primeira abertura.
            AppContainer.securePreferences.lastBackgroundedAt = 0L
            AppLock.locked.value = false
        }.exceptionOrNull()?.stackTraceToString()

        val crashReport = bootFailure ?: CrashReporter.consumeLastCrash(this)
        enableEdgeToEdge()
        setContent {
            BioAcupuntTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showCrash by remember { mutableStateOf(crashReport != null) }
                    val locked by AppLock.locked
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showCrash && crashReport != null) {
                            CrashReportScreen(report = crashReport, onContinue = { showCrash = false })
                        } else {
                            BioAcupuntNavHost()
                        }
                        // Re-lock por inatividade: superfície OPACA cobrindo tudo
                        // (esconde o conteúdo clínico) + a tela de desbloqueio.
                        // Ao autenticar, o NavHost por baixo reaparece exatamente
                        // onde estava — a pilha nunca saiu de composição.
                        if (locked) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background,
                            ) {
                                BiometricLockScreen(onUnlocked = {
                                    runCatching { AppContainer.securePreferences.lastBackgroundedAt = 0L }
                                    AppLock.locked.value = false
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    /** Carimba a hora em que o app foi para segundo plano — base do auto-bloqueio. */
    override fun onStop() {
        super.onStop()
        runCatching {
            val prefs = AppContainer.securePreferences
            // Só arma o cronômetro se há sessão ativa; sem login não há o que travar.
            if (prefs.isLoggedIn && !AppLock.locked.value) {
                prefs.lastBackgroundedAt = System.currentTimeMillis()
            }
        }
    }

    /** Ao voltar ao primeiro plano, re-trava se passou tempo demais em background. */
    override fun onStart() {
        super.onStart()
        runCatching {
            val prefs = AppContainer.securePreferences
            val last = prefs.lastBackgroundedAt
            if (prefs.isLoggedIn && last > 0L) {
                val elapsedMin = (System.currentTimeMillis() - last) / 60_000L
                if (elapsedMin >= prefs.autoLockMinutes) {
                    AppLock.locked.value = true
                }
            }
        }
    }
}
