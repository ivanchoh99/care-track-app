package com.app.caretrack.auth.model

enum class Role(val label: String, val value: Int) {
    SYSTEM_ADMIN("Administrador del Sistema", 1),
    FAMILY_ADMIN("Administrador de Familia", 2),
    CAREGIVER("Cuidador", 3),
    VIEWER("Observador", 4);

    companion object {
        fun fromValue(value: Int): Role? = entries.find { it.value == value }
    }
}

enum class TypeDocument(val label: String, val value: Int) {
    CITIZEN_DOCUMENT("Cédula de Ciudadanía", 1),
    PASSPORT("Pasaporte", 2);

    companion object {
        fun fromValue(value: Int): TypeDocument? = entries.find { it.value == value }
    }
}

data class UserModel(
    val id: Long,
    val uuid: String,
    val telegramId: Long,
    val username: String,
    val typeDocument: TypeDocument,
    val document: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: Long,
    val familyId: Long,
    val role: Role,
    val isActive: Boolean
) {
    val fullName: String get() = "$firstName $lastName"
}

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long
)

data class UserSession(
    val user: UserModel,
    val tokens: AuthTokens
)

data class LoginRequest(
    val telegramId: Long
)

data class AuthResponse(
    val user: UserModel,
    val tokens: AuthTokens
)