package com.app.caretrack.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.caretrack.auth.model.AuthTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

actual class TokenStore {
    private var context: Context? = null
    
    fun initialize(ctx: Context) {
        context = ctx
    }
    
    private val store: DataStore<Preferences>
        get() = context?.authDataStore ?: throw IllegalStateException("TokenStore not initialized")
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val EXPIRES_AT_KEY = longPreferencesKey("expires_at")
    }
    
    actual suspend fun saveTokens(tokens: AuthTokens) {
        store.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = tokens.accessToken
            prefs[REFRESH_TOKEN_KEY] = tokens.refreshToken
            prefs[EXPIRES_AT_KEY] = tokens.expiresAt
        }
    }
    
    actual suspend fun getTokens(): AuthTokens? {
        val prefs = store.data.first()
        val accessToken = prefs[ACCESS_TOKEN_KEY] ?: return null
        val refreshToken = prefs[REFRESH_TOKEN_KEY] ?: return null
        val expiresAt = prefs[EXPIRES_AT_KEY] ?: return null
        
        return AuthTokens(accessToken, refreshToken, expiresAt)
    }
    
    actual suspend fun clearTokens() {
        store.edit { it.clear() }
    }
    
    actual val accessToken: Flow<String?>
        get() = store.data.map { prefs -> prefs[ACCESS_TOKEN_KEY] }
}

actual fun createTokenStore(): TokenStore = TokenStore()