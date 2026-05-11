package com.app.caretrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.app.caretrack.auth.data.MockAuthRepository
import com.app.caretrack.auth.data.SessionManager
import com.app.caretrack.chat.ChatRepository
import com.app.caretrack.chat.getRoomDatabase
import com.app.caretrack.chat.instantiateDatabaseBuilder
import com.app.caretrack.chat.network.ApiService
import com.app.caretrack.family.data.createFamilyContextManager
import com.app.caretrack.media.file.FileStorageManager
import com.app.caretrack.navigation.AppNavigation
import com.app.caretrack.profile.data.MockProfileRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current

            val repository = remember {
                val builder = instantiateDatabaseBuilder(context)
                val database = getRoomDatabase(builder)
                val fileManager = FileStorageManager(context)
                ChatRepository(database.chatDao(), fileManager)
            }

            val sessionManager = remember { SessionManager() }
            val apiService = remember { ApiService() }

            val familyContext = remember { createFamilyContextManager(context) }

            // MockAuthRepository: valida ivanchoh99@gmail.com / ivanchoh99@gmail.com
            // Reemplazar por AuthRepositoryImpl cuando el backend esté listo
            val authRepository = remember { MockAuthRepository(sessionManager = sessionManager) }

            val profileRepository = remember { MockProfileRepository() }

            AppNavigation(
                sessionManager = sessionManager,
                authRepository = authRepository,
                familyContext = familyContext,
                repository = repository,
                profileRepository = profileRepository
            )
        }
    }
}
