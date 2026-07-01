package com.bioacupunt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.navigation.BioAcupuntNavHost
import com.bioacupunt.ui.theme.BioAcupuntTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(applicationContext)
        AppContainer.seedDemoDataIfNeeded()
        enableEdgeToEdge()
        setContent {
            BioAcupuntTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BioAcupuntNavHost()
                }
            }
        }
    }
}
