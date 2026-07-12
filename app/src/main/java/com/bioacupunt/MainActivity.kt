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
        AppContainer.init(applicationContext)
        AppContainer.seedDemoDataIfNeeded()
        val crashReport = CrashReporter.consumeLastCrash(this)
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
