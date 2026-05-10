package com.app.caretrack.chat.network

data class FileUploadResult(
    val url: String,
    val fileName: String,
    val success: Boolean,
    val error: String? = null
)
