package com.app.caretrack.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.caretrack.auth.data.AuthRepository
import com.app.caretrack.auth.model.TypeDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    data object Idle : RegisterUiState()
    data object Loading : RegisterUiState()
    data object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

data class RegisterFieldErrors(
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val documentError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val invitationCodeError: String? = null
) {
    val hasErrors get() = firstNameError != null || lastNameError != null ||
            emailError != null || phoneError != null || documentError != null ||
            passwordError != null || confirmPasswordError != null || invitationCodeError != null
}

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _fieldErrors = MutableStateFlow(RegisterFieldErrors())
    val fieldErrors: StateFlow<RegisterFieldErrors> = _fieldErrors.asStateFlow()

    // 0 = sin input, 1 = débil, 2 = media, 3 = fuerte
    private val _passwordStrength = MutableStateFlow(0)
    val passwordStrength: StateFlow<Int> = _passwordStrength.asStateFlow()

    fun validateFirstName(value: String) {
        _fieldErrors.value = _fieldErrors.value.copy(
            firstNameError = when {
                value.isBlank() -> null
                value.trim().length < 2 -> "Mínimo 2 caracteres"
                !value.matches(NAME_REGEX) -> "Solo letras, espacios y tildes"
                else -> null
            }
        )
    }

    fun validateLastName(value: String) {
        _fieldErrors.value = _fieldErrors.value.copy(
            lastNameError = when {
                value.isBlank() -> null
                value.trim().length < 2 -> "Mínimo 2 caracteres"
                !value.matches(NAME_REGEX) -> "Solo letras, espacios y tildes"
                else -> null
            }
        )
    }

    fun validateEmail(email: String) {
        _fieldErrors.value = _fieldErrors.value.copy(
            emailError = when {
                email.isBlank() -> null
                !email.matches(EMAIL_REGEX) -> "Formato de correo inválido"
                else -> null
            }
        )
    }

    fun validatePhone(value: String) {
        _fieldErrors.value = _fieldErrors.value.copy(
            phoneError = when {
                value.isBlank() -> null
                !value.all { it.isDigit() } -> "Solo dígitos"
                value.length !in 7..15 -> "Entre 7 y 15 dígitos"
                else -> null
            }
        )
    }

    fun validateDocument(typeDocument: TypeDocument, value: String) {
        _fieldErrors.value = _fieldErrors.value.copy(
            documentError = when {
                value.isBlank() -> null
                typeDocument == TypeDocument.CITIZEN_DOCUMENT &&
                        (!value.all { it.isDigit() } || value.length !in 6..10) ->
                    "CC: 6-10 dígitos"
                typeDocument == TypeDocument.PASSPORT &&
                        !value.all { it.isLetterOrDigit() } ->
                    "Solo letras y números"
                else -> null
            }
        )
    }

    fun validatePassword(password: String) {
        _passwordStrength.value = when {
            password.isBlank() -> 0
            password.length < 8 -> 1
            !password.any { it.isUpperCase() } || !password.any { it.isDigit() } -> 2
            else -> 3
        }
        _fieldErrors.value = _fieldErrors.value.copy(
            passwordError = when {
                password.isBlank() -> null
                password.length < 8 -> "Mínimo 8 caracteres"
                !password.any { it.isUpperCase() } -> "Incluye al menos una mayúscula"
                !password.any { it.isDigit() } -> "Incluye al menos un número"
                else -> null
            }
        )
    }

    fun validateConfirmPassword(password: String, confirm: String) {
        _fieldErrors.value = _fieldErrors.value.copy(
            confirmPasswordError = when {
                confirm.isBlank() -> null
                confirm != password -> "Las contraseñas no coinciden"
                else -> null
            }
        )
    }

    fun validateInvitationCode(code: String) {
        _fieldErrors.value = _fieldErrors.value.copy(
            invitationCodeError = when {
                code.isBlank() -> null
                !code.matches(UUID_REGEX) -> "Formato inválido (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)"
                else -> null
            }
        )
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        phone: String,
        typeDocument: TypeDocument,
        document: String,
        invitationCode: String?
    ) {
        validateFirstName(firstName)
        validateLastName(lastName)
        validateEmail(email)
        validatePhone(phone)
        validateDocument(typeDocument, document)
        validatePassword(password)
        validateConfirmPassword(password, confirmPassword)
        invitationCode?.let { validateInvitationCode(it) }

        val requiredBlank = firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                password.isBlank() || confirmPassword.isBlank() || document.isBlank()
        if (requiredBlank) {
            _uiState.value = RegisterUiState.Error("Completa todos los campos obligatorios.")
            return
        }
        if (_fieldErrors.value.hasErrors) return

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            authRepository.register(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                email = email.trim(),
                password = password,
                phone = phone.toLongOrNull() ?: 0L,
                typeDocument = typeDocument,
                document = document.trim(),
                invitationCode = invitationCode?.trim()?.ifBlank { null }
            ).fold(
                onSuccess = { _uiState.value = RegisterUiState.Success },
                onFailure = { _uiState.value = RegisterUiState.Error(it.message ?: "Error al crear la cuenta.") }
            )
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val NAME_REGEX = Regex("^[A-Za-zÀ-ÿ ]+$")
        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
