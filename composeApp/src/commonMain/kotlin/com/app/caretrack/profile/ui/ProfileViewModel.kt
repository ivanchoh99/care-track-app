package com.app.caretrack.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.caretrack.auth.model.TypeDocument
import com.app.caretrack.auth.model.UserModel
import com.app.caretrack.profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: UserModel) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    data object Saving : ProfileUiState()
    data object Saved : ProfileUiState()
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val _firstName = MutableStateFlow("")
    var firstName: String get() = _firstName.value; set(value) { _firstName.value = value }
    
    private val _lastName = MutableStateFlow("")
    var lastName: String get() = _lastName.value; set(value) { _lastName.value = value }
    
    private val _email = MutableStateFlow("")
    var email: String get() = _email.value; set(value) { _email.value = value }
    
    private val _phone = MutableStateFlow("")
    var phone: String get() = _phone.value; set(value) { _phone.value = value }
    
    private val _typeDocument = MutableStateFlow(TypeDocument.CITIZEN_DOCUMENT)
    var typeDocument: TypeDocument get() = _typeDocument.value; set(value) { _typeDocument.value = value }
    
    private val _document = MutableStateFlow("")
    var document: String get() = _document.value; set(value) { _document.value = value }
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = profileRepository.loadProfile()
            result.fold(
                onSuccess = { user ->
                    _firstName.value = user.firstName
                    _lastName.value = user.lastName
                    _email.value = user.email
                    _phone.value = user.phone.toString()
                    _typeDocument.value = user.typeDocument
                    _document.value = user.document
                    _uiState.value = ProfileUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Error loading profile")
                }
            )
        }
    }
    
    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Saving
            
            val result = profileRepository.updateProfile(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone.toLongOrNull() ?: 0L,
                typeDocument = typeDocument,
                document = document
            )
            
            result.fold(
                onSuccess = { user ->
                    _uiState.value = ProfileUiState.Saved
                    _uiState.value = ProfileUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Error saving profile")
                }
            )
        }
    }
    
    fun resetSavedState() {
        if (_uiState.value is ProfileUiState.Success) {
            // Already in success state
        }
    }
}