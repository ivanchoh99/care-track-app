package com.app.caretrack.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caretrack.composeapp.generated.resources.Res
import caretrack.composeapp.generated.resources.audio_file_24px
import caretrack.composeapp.generated.resources.image_24px
import caretrack.composeapp.generated.resources.picture_as_pdf_24px
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AttachmentMenu(onSelect: (MessageType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AttachmentItem(Res.drawable.image_24px, "Imagen") { onSelect(MessageType.IMAGE) }
        AttachmentItem(Res.drawable.picture_as_pdf_24px, "PDF") { onSelect(MessageType.DOCUMENT) }
        AttachmentItem(Res.drawable.audio_file_24px, "Audio") { onSelect(MessageType.AUDIO) }
    }
}

@Composable
fun AttachmentItem(
    iconRes: DrawableResource,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
