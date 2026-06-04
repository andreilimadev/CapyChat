package com.andreilima.capychat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andreilima.capychat.data.firebase.FirebaseService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class LoggedIn(val uid: String, val username: String) : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = FirebaseService.auth.currentUser
        if (user != null) {
            loadUserData(user.uid)
        } else {
            _state.value = AuthState.LoggedOut
        }
    }

    private fun loadUserData(uid: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val userData = FirebaseService.getUser(uid)
            if (userData != null) {
                _state.value = AuthState.LoggedIn(uid, userData.username)
            } else {
                _state.value = AuthState.LoggedOut
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Preencha todos os campos")
            return
        }

        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = FirebaseService.login(email, password)
            if (result.isSuccess) {
                val uid = FirebaseService.auth.currentUser?.uid
                if (uid != null) {
                    loadUserData(uid)
                } else {
                    _state.value = AuthState.Error("Falha ao recuperar ID do usuário")
                }
            } else {
                _state.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Erro no login")
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Preencha todos os campos")
            return
        }

        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = FirebaseService.register(name, email, password)
            if (result.isSuccess) {
                val uid = FirebaseService.auth.currentUser?.uid
                if (uid != null) {
                    loadUserData(uid)
                } else {
                    _state.value = AuthState.Error("Falha ao recuperar ID do usuário")
                }
            } else {
                _state.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Erro no cadastro")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            FirebaseService.logout()
            _state.value = AuthState.LoggedOut
        }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Idle
        }
    }
}
