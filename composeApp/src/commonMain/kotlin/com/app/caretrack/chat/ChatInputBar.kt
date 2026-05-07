package com.app.caretrack.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import caretrack.composeapp.generated.resources.Res
import caretrack.composeapp.generated.resources.add_24px
import caretrack.composeapp.generated.resources.mic_24px
import caretrack.composeapp.generated.resources.send_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onAttachmentSelected: (MessageType) -> Unit,
    onVoiceNoteStart: () -> Unit,
    onVoiceNoteEnd: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
    ) {
        // Menú emergente de adjuntos
        AnimatedVisibility(visible = showMenu) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp
            ) {
                AttachmentMenu { type ->
                    onAttachmentSelected(type)
                    showMenu = false
                }
            }
        }

        // La barra de texto estilo "Píldora"
        Surface(
            shape = RoundedCornerShape(28.dp), // Totalmente redondeada
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Micrófono o Adjuntar
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        painterResource(Res.drawable.add_24px),
                        "Adjuntar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mensaje...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )

                if (text.isNotBlank()) {
                    IconButton(onClick = {
                        onSendMessage(text)
                        text = ""
                    }) {
                        Icon(
                            painterResource(Res.drawable.send_24px),
                            "Enviar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = { /* Iniciar audio */ }) {
                        Icon(
                            painterResource(Res.drawable.mic_24px),
                            "Audio",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}