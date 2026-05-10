package com.app.caretrack.profile.data

import com.app.caretrack.auth.model.UserModel
import com.app.caretrack.auth.model.TypeDocument
import com.app.caretrack.chat.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ProfileRepository {
    val currentProfile: StateFlow<UserModel?>
    suspend fun loadProfile(): Result<UserModel>
    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        phone: Long,
        typeDocument: TypeDocument,
        document: String
    ): Result<UserModel>
}

class ProfileRepositoryImpl(
    private val apiService: ApiService,
    private val getToken: suspend () -> String?
) : ProfileRepository {
    
    private val _currentProfile = MutableStateFlow<UserModel?>(null)
    override val currentProfile: StateFlow<UserModel?> = _currentProfile.asStateFlow()
    
    override suspend fun loadProfile(): Result<UserModel> {
        return try {
            val token = getToken() ?: throw Exception("No token available")
            val result = apiService.getCurrentUser(token)
            result.fold(
                onSuccess = { user ->
                    _currentProfile.value = user
                    Result.success(user)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        phone: Long,
        typeDocument: TypeDocument,
        document: String
    ): Result<UserModel> {
        return try {
            val token = getToken() ?: throw Exception("No token available")
            // TODO: Implement actual API call when backend supports it
            val currentUser = _currentProfile.value ?: throw Exception("No profile loaded")
            
            val updatedUser = currentUser.copy(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                typeDocument = typeDocument,
                document = document
            )
            
            _currentProfile.value = updatedUser
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}