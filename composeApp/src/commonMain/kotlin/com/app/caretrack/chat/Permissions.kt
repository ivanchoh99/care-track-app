package com.app.caretrack.chat

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher

interface PermissionRequestLauncher {
    fun launch()
}