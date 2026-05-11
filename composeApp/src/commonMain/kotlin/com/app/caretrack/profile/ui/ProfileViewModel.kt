package com.app.caretrack.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.caretrack.auth.model.TypeDocument
import com.app.caretrack.auth.model.UserModel
import com.app.caretrack.profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: UserModel) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    data object Saving : ProfileUiState()
}

data class ProfileFormErrors(
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val documentError: String? = null
) {
    val hasErrors get() = firstNameError != null || lastNameError != null ||
            emailError != null || phoneError != null || documentError != null
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _formErrors = MutableStateFlow(ProfileFormErrors())
    val formErrors: StateFlow<ProfileFormErrors> = _formErrors.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val _firstName = MutableStateFlow("")
    var firstName: String
        get() = _firstName.value
        set(value) {
            _firstName.value = value
            validateFirstName(value)
        }

    private val _lastName = MutableStateFlow("")
    var lastName: String
        get() = _lastName.value
        set(value) {
            _lastName.value = value
            validateLastName(value)
        }

    private val _email = MutableStateFlow("")
    var email: String
        get() = _email.value
        set(value) {
            _email.value = value
            validateEmail(value)
        }

    private val _phone = MutableStateFlow("")
    var phone: String
        get() = _phone.value
        set(value) {
            _phone.value = value
            validatePhone(value)
        }

    private val _typeDocument = MutableStateFlow(TypeDocument.CITIZEN_DOCUMENT)
    var typeDocument: TypeDocument
        get() = _typeDocument.value
        set(value) {
            _typeDocument.value = value
            validateDocument(_document.value)
        }

    private val _document = MutableStateFlow("")
    var document: String
        get() = _document.value
        set(value) {
            _document.value = value
            validateDocument(value)
        }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            profileRepository.loadProfile().fold(
                onSuccess = { user ->
                    _firstName.value = user.firstName
                    _lastName.value = user.lastName
                    _email.value = user.email
                    _phone.value = user.phone.toString()
                    _typeDocument.value = user.typeDocument
                    _document.value = user.document
                    _formErrors.value = ProfileFormErrors()
                    _uiState.value = ProfileUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Error al cargar el perfil")
                }
            )
        }
    }

    fun saveProfile() {
        if (!validateAll()) return
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Saving
            profileRepository.updateProfile(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone.toLongOrNull() ?: 0L,
                typeDocument = typeDocument,
                document = document
            ).fold(
                onSuccess = { user ->
                    _uiState.value = ProfileUiState.Success(user)
                    _savedEvent.emit(Unit)
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Error al guardar el perfil")
                }
            )
        }
    }

    fun validateFirstName(value: String) {
        _formErrors.value = _formErrors.value.copy(
            firstNameError = when {
                value.isBlank() -> "El nombre es obligatorio"
                !value.matches(NAME_REGEX) -> "Solo letras y espacios"
                else -> null
            }
        )
    }

    fun validateLastName(value: String) {
        _formErrors.value = _formErrors.value.copy(
            lastNameError = when {
                value.isBlank() -> "El apellido es obligatorio"
                !value.matches(NAME_REGEX) -> "Solo letras y espacios"
                else -> null
            }
        )
    }

    fun validateEmail(value: String) {
        _formErrors.value = _formErrors.value.copy(
            emailError = when {
                value.isBlank() -> "El correo es obligatorio"
                !value.matches(EMAIL_REGEX) -> "Formato de correo inválido"
                else -> null
            }
        )
    }

    fun validatePhone(value: String) {
        _formErrors.value = _formErrors.value.copy(
            phoneError = when {
                value.isBlank() -> null
                !value.all { it.isDigit() } -> "Solo dígitos"
                value.length !in 7..15 -> "Entre 7 y 15 dígitos"
                else -> null
            }
        )
    }

    fun validateDocument(value: String) {
        _formErrors.value = _formErrors.value.copy(
            documentError = when {
                value.isBlank() -> "El documento es obligatorio"
                typeDocument == TypeDocument.CITIZEN_DOCUMENT &&
                        (!value.all { it.isDigit() } || value.length !in 6..10) ->
                    "CC: 6-10 dígitos"
                else -> null
            }
        )
    }

    private fun validateAll(): Boolean {
        validateFirstName(firstName)
        validateLastName(lastName)
        validateEmail(email)
        validatePhone(phone)
        validateDocument(document)
        return !_formErrors.value.hasErrors
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val NAME_REGEX = Regex("^[A-Za-zÀ-ÿ ]+$")
    }
}
