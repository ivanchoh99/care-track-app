package com.app.caretrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.app.caretrack.chat.getRoomDatabase
import com.app.caretrack.chat.instantiateDatabaseBuilder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val database = remember {
                val builder = instantiateDatabaseBuilder(context)
                getRoomDatabase(builder)
            }
            App(dao = database.chatDao())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    val database = remember {
        val builder = instantiateDatabaseBuilder(context)
        getRoomDatabase(builder)
    }
    App(database.chatDao())
}