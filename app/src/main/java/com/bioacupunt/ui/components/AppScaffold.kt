package com.bioacupunt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bioacupunt.ui.design.DesignTokens
import com.bioacupunt.ui.navigation.Screen

@Composable
fun AppScaffold(
    selectedRoute: String,
    topBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)
        ) {
            Surface(tonalElevation = DesignTokens.ElevationTokens.level1) {
                topBar()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = DesignTokens.Spacing.screen)
        ) {
            content()
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))
        }

        Surface(tonalElevation = DesignTokens.ElevationTokens.level2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.sm)
            ) {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item.route == selectedRoute,
                        onClick = { },
                        icon = { },
                        label = { Text(text = item.label) }
                    )
                }
            }
        }
    }
}
