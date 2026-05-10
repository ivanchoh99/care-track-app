// =============================================================================
// ⚠️ ARCHIVO DUPLICADO — PENDIENTE DE ELIMINAR
// =============================================================================
// Este archivo es una copia de `chat/network/FileUploadResult.kt`.
// TODO: Eliminar este archivo. El código activo está en:
//       composeApp/src/commonMain/.../chat/network/FileUploadResult.kt
// =============================================================================
package com.app.caretrack.api

data class FileUploadResult(
    val url: String,
    val fileName: String,
    val success: Boolean,
    val error: String? = null
)
