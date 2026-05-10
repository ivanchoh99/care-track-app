package com.app.caretrack.common

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }
    
    return object : PermissionRequestLauncher {
        override fun launch() {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

actual fun checkInitialAudioPermission(): Boolean {
    return false
}