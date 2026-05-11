package com.app.caretrack.profile.data

import com.app.caretrack.auth.model.TypeDocument
import com.app.caretrack.auth.model.UserModel
import com.app.caretrack.common.mock.MockData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockProfileRepository : ProfileRepository {

    private val _currentProfile = MutableStateFlow<UserModel?>(MockData.user)
    override val currentProfile: StateFlow<UserModel?> = _currentProfile.asStateFlow()

    override suspend fun loadProfile(): Result<UserModel> {
        delay(300)
        return Result.success(MockData.user)
    }

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        phone: Long,
        typeDocument: TypeDocument,
        document: String
    ): Result<UserModel> {
        delay(300)
        val updated = (_currentProfile.value ?: MockData.user).copy(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            typeDocument = typeDocument,
            document = document
        )
        _currentProfile.value = updated
        return Result.success(updated)
    }
}
