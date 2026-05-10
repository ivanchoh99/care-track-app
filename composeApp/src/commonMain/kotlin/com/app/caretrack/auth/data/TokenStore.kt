package com.app.caretrack.auth.data

import com.app.caretrack.auth.model.AuthTokens
import kotlinx.coroutines.flow.Flow

expect class TokenStore() {
    suspend fun saveTokens(tokens: AuthTokens)
    suspend fun getTokens(): AuthTokens?
    suspend fun clearTokens()
    val accessToken: Flow<String?>
}

expect fun createTokenStore(): TokenStore