package com.app.caretrack.auth.data

import com.app.caretrack.auth.model.AuthTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

actual class TokenStore {
    private var tokens: AuthTokens? = null
    private val _accessToken = MutableStateFlow<String?>(null)
    
    actual suspend fun saveTokens(newTokens: AuthTokens) {
        tokens = newTokens
        _accessToken.value = newTokens.accessToken
    }
    
    actual suspend fun getTokens(): AuthTokens? {
        return tokens
    }
    
    actual suspend fun clearTokens() {
        tokens = null
        _accessToken.value = null
    }
    
    actual val accessToken: Flow<String?> = _accessToken
}

actual fun createTokenStore(): TokenStore = TokenStore()