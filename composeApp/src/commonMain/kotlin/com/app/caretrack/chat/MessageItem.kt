package com.app.caretrack.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageItem(message: ChatMessage) {
    // 2. CORRECCIÓN DE ALINEACIÓN
    val isMine = message.isMine
    val alignment =
        if (isMine) Alignment.End else Alignment.Start // A la derecha si es mío, a la izquierda si no

    val bubbleColor =
        if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor =
        if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) // Esquina inferior derecha en punta
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp) // Esquina inferior izquierda en punta
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment // APLICAMOS LA ALINEACIÓN AQUÍ
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp) // Evita que la burbuja ocupe toda la pantalla
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}