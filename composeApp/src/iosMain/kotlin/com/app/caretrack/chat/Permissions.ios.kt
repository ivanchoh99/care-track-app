package com.app.caretrack.chat

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    return object : PermissionRequestLauncher {
        override fun launch() {
            // En iOS el sistema de permisos es distinto,
            // por ahora simulamos que siempre acepta o manejamos luego
            onResult(true)
        }
    }
}