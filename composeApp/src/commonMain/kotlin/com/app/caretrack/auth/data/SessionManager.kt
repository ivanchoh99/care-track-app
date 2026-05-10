package com.app.caretrack.auth.data

import com.app.caretrack.auth.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    val isLoggedIn: Boolean get() = _session.value != null

    fun setSession(session: UserSession?) {
        _session.value = session
    }

    fun clearSession() {
        _session.value = null
    }

    fun getCurrentUser() = _session.value?.user

    fun getAccessToken() = _session.value?.tokens?.accessToken
}