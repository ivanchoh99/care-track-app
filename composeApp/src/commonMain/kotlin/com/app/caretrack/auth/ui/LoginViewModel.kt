package com.app.caretrack.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.caretrack.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    fun validateEmail(email: String) {
        _emailError.value = if (email.isNotBlank() && !email.matches(EMAIL_REGEX))
            "Formato de correo inválido"
        else null
    }

    fun clearEmailError() {
        _emailError.value = null
    }

    fun login(email: String, password: String) {
        if (email.isNotBlank() && !email.matches(EMAIL_REGEX)) {
            _emailError.value = "Formato de correo inválido"
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.login(email, password)
            _uiState.value = result.fold(
                onSuccess = { LoginUiState.Success },
                onFailure = { LoginUiState.Error(it.message ?: "Error al iniciar sesión") }
            )
        }
    }

    fun restoreSession() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val restored = authRepository.restoreSession()
            _uiState.value = if (restored) LoginUiState.Success else LoginUiState.Idle
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
