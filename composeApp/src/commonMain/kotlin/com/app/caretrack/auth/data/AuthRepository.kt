package com.app.caretrack.auth.data

import com.app.caretrack.auth.model.AuthResponse
import com.app.caretrack.auth.model.AuthTokens
import com.app.caretrack.auth.model.LoginRequest
import com.app.caretrack.auth.model.UserModel
import com.app.caretrack.auth.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<UserSession?>
    val isLoggedIn: Boolean

    suspend fun login(email: String, password: String): Result<UserSession>
    suspend fun register(name: String, email: String, password: String, invitationCode: String?): Result<UserSession>
    suspend fun logout()
    suspend fun refreshToken(): Result<AuthTokens>
    suspend fun restoreSession(): Boolean
}

class AuthRepositoryImpl(
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager,
    private val apiService: com.app.caretrack.chat.network.ApiService
) : AuthRepository {
    
    override val session: StateFlow<UserSession?> = sessionManager.session
    override val isLoggedIn: Boolean get() = sessionManager.isLoggedIn
    
    override suspend fun register(name: String, email: String, password: String, invitationCode: String?): Result<UserSession> {
        return Result.failure(NotImplementedError("register not implemented in real backend yet"))
    }

    override suspend fun login(email: String, password: String): Result<UserSession> {
        return try {
            val loginResult = apiService.login(LoginRequest(email, password))
            loginResult.fold(
                onSuccess = { response ->
                    val userSession = UserSession(
                        user = response.user,
                        tokens = response.tokens
                    )
                    tokenStore.saveTokens(response.tokens)
                    sessionManager.setSession(userSession)
                    Result.success(userSession)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) { }
        tokenStore.clearTokens()
        sessionManager.clearSession()
    }
    
    override suspend fun refreshToken(): Result<AuthTokens> {
        return try {
            val currentTokens = tokenStore.getTokens() ?: throw Exception("No tokens stored")
            val refreshResult = apiService.refreshToken(currentTokens.refreshToken)
            refreshResult.fold(
                onSuccess = { response ->
                    tokenStore.saveTokens(response.tokens)
                    Result.success(response.tokens)
                },
                onFailure = {
                    logout()
                    Result.failure(it)
                }
            )
        } catch (e: Exception) {
            logout()
            Result.failure(e)
        }
    }
    
    override suspend fun restoreSession(): Boolean {
        val tokens = tokenStore.getTokens() ?: return false
        return try {
            val userResult = apiService.getCurrentUser(tokens.accessToken)
            userResult.fold(
                onSuccess = { user ->
                    sessionManager.setSession(UserSession(user, tokens))
                    true
                },
                onFailure = {
                    val refreshed = refreshToken()
                    refreshed.isSuccess
                }
            )
        } catch (e: Exception) {
            val refreshed = refreshToken()
            refreshed.isSuccess
        }
    }
}