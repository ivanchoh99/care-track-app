package com.app.caretrack.auth.data

import com.app.caretrack.auth.model.AuthTokens
import com.app.caretrack.auth.model.TypeDocument
import com.app.caretrack.auth.model.UserSession
import com.app.caretrack.common.mock.MockData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class MockAuthRepository(
    private val sessionManager: SessionManager
) : AuthRepository {

    override val session: StateFlow<UserSession?> = sessionManager.session
    override val isLoggedIn: Boolean get() = sessionManager.isLoggedIn

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phone: Long,
        typeDocument: TypeDocument,
        document: String,
        invitationCode: String?
    ): Result<UserSession> {
        delay(800)
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Todos los campos son obligatorios."))
        }
        val tokens = AuthTokens("mock-access-token", "mock-refresh-token", Long.MAX_VALUE)
        val userSession = UserSession(
            user = MockData.user.copy(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                typeDocument = typeDocument,
                document = document
            ),
            tokens = tokens
        )
        sessionManager.setSession(userSession)
        return Result.success(userSession)
    }

    override suspend fun login(email: String, password: String): Result<UserSession> {
        delay(800)
        return if (email.trim() == MockData.MOCK_EMAIL && password == MockData.MOCK_PASSWORD) {
            val tokens = AuthTokens(
                accessToken = "mock-access-token",
                refreshToken = "mock-refresh-token",
                expiresAt = Long.MAX_VALUE
            )
            val userSession = UserSession(user = MockData.user, tokens = tokens)
            sessionManager.setSession(userSession)
            Result.success(userSession)
        } else {
            Result.failure(Exception("Credenciales incorrectas. Verifica tu email y contraseña."))
        }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun refreshToken(): Result<AuthTokens> {
        return Result.success(
            AuthTokens("mock-access-token", "mock-refresh-token", Long.MAX_VALUE)
        )
    }

    override suspend fun restoreSession(): Boolean {
        return sessionManager.isLoggedIn
    }
}
