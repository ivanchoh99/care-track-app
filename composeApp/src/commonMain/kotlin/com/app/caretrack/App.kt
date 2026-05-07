package com.app.caretrack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.caretrack.chat.ChatInputBar
import com.app.caretrack.chat.ChatViewModel
import com.app.caretrack.chat.MessageItem
import kotlinx.coroutines.launch

@Preview
@Composable
fun App() {
    val chatViewModel: ChatViewModel = viewModel { ChatViewModel() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    MaterialTheme {

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            bottomBar = {
                ChatInputBar(
                    onSendMessage = { text ->
                        chatViewModel.sendMessage(text)
                        scope.launch {
                            listState.animateScrollToItem(chatViewModel.messages.size - 1)
                        }
                    },
                    onAttachmentSelected = { /* Próximamente */ },
                    onVoiceNoteStart = { /* Próximamente */ },
                    onVoiceNoteEnd = { /* Próximamente */ }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatViewModel.messages) { message ->
                    MessageItem(message)
                }
            }
        }
    }
}
