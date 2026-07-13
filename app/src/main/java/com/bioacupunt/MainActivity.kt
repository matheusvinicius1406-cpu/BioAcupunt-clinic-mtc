package com.bioacupunt

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.bioacupunt.ui.navigation.BioAcupuntNavHost
import com.bioacupunt.ui.theme.BioAcupuntTheme

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
                    if (showCrash && crashReport != null) {
                        CrashReportScreen(report = crashReport, onContinue = { showCrash = false })
                    } else {
                        BioAcupuntNavHost()
                    }
                }
            }
        }
    }
}
