package com.app.caretrack

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.app.caretrack.chat.ChatRepository
import com.app.caretrack.chat.FileStorageManager
import com.app.caretrack.chat.getRoomDatabase
import com.app.caretrack.chat.instantiateDatabaseBuilder

fun MainViewController() = ComposeUIViewController {
    val repository = remember {
        val database = getRoomDatabase(instantiateDatabaseBuilder(null))
        val fileManager = FileStorageManager(null)
        ChatRepository(database.chatDao(), fileManager)
    }
    App(repository = repository)
}
