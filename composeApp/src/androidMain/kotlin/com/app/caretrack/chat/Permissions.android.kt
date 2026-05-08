package com.app.caretrack.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    return object : PermissionRequestLauncher {
        override fun launch() {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}