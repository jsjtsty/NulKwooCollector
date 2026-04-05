package com.nulstudio.kwoocollector.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.request.LoginRequest
import com.nulstudio.kwoocollector.net.model.request.RegisterRequest
import com.nulstudio.kwoocollector.push.JPushService
import com.nulstudio.kwoocollector.util.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val inviteCode: String = "",
    val isLoginMode: Boolean = true,
    val isSubmitting: Boolean = false
)

sealed class LoginUiEvent {
    data class ShowToast(val message: String) : LoginUiEvent()
    object NavigateToMain : LoginUiEvent()
}

class LoginViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun switchMode(index: Int) {
        _uiState.update { it.copy(isLoginMode = index == 0) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun updateInviteCode(value: String) {
        _uiState.update { it.copy(inviteCode = value) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val username = state.username.trim()
        val password = state.password
        val inviteCode = state.inviteCode.trim()

        if (username.isBlank()) {
            emitToast("请输入账号")
            return
        }
        if (password.isBlank()) {
            emitToast("请输入密码")
            return
        }
        if (!state.isLoginMode && inviteCode.isBlank()) {
            emitToast("请输入邀请码")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            runCatching {
                if (state.isLoginMode) {
                    apiService.login(LoginRequest(username = username, password = password))
                } else {
                    apiService.register(
                        RegisterRequest(
                            username = username,
                            password = password,
                            inviteCode = inviteCode
                        )
                    )
                }
            }.onSuccess { response ->
                val token = response.result
                if (response.code == 0 && !token.isNullOrBlank()) {
                    tokenManager.saveToken(token)
                    JPushService.bindAlias(context, username)
                    _uiEvent.emit(LoginUiEvent.NavigateToMain)
                } else {
                    _uiEvent.emit(
                        LoginUiEvent.ShowToast(
                            response.message ?: if (state.isLoginMode) "登录失败" else "注册失败"
                        )
                    )
                }
            }.onFailure { error ->
                _uiEvent.emit(
                    LoginUiEvent.ShowToast(
                        error.localizedMessage ?: "网络连接异常，请稍后重试"
                    )
                )
            }

            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(LoginUiEvent.ShowToast(message))
        }
    }
}
