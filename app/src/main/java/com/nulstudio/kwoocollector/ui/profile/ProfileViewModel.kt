package com.nulstudio.kwoocollector.ui.profile

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.request.ChangePasswordRequest
import com.nulstudio.kwoocollector.push.JPushService
import com.nulstudio.kwoocollector.util.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isChangingPassword: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val username: String = "加载中...",
    val role: String = "...",
    val localVersionName: String = "",
    val localVersionCode: Long = 0L
)

sealed class ProfileUiEvent {
    data object NavigateToLogin : ProfileUiEvent()
    data object PasswordChanged : ProfileUiEvent()
    data class ShowUpdateDialog(
        val newVersion: String,
        val build: Int,
        val downloadUrl: String
    ) : ProfileUiEvent()

    data class ShowToast(val message: String) : ProfileUiEvent()
}

class ProfileViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadLocalAppVersion()
        fetchUserProfile()
    }

    private fun loadLocalAppVersion() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _uiState.update {
                it.copy(
                    localVersionName = packageInfo.versionName ?: "未知版本",
                    localVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
                )
            }
        } catch (_: PackageManager.NameNotFoundException) {
        }
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            runCatching {
                apiService.fetchProfile()
            }.onSuccess { response ->
                if (response.code == 0 && response.result != null) {
                    JPushService.bindAlias(context, response.result.username)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            username = response.result.username,
                            role = response.result.role
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(ProfileUiEvent.ShowToast(response.message ?: "获取个人信息失败"))
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(ProfileUiEvent.ShowToast("网络连接异常，无法刷新个人信息"))
            }
        }
    }

    fun checkUpdate() {
        if (_uiState.value.isCheckingUpdate) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }

            runCatching {
                apiService.fetchUpdates()
            }.onSuccess { response ->
                if (response.code == 0 && response.result != null) {
                    val serverUpdate = response.result
                    if (serverUpdate.build > _uiState.value.localVersionCode) {
                        _uiEvent.emit(
                            ProfileUiEvent.ShowUpdateDialog(
                                newVersion = serverUpdate.version,
                                build = serverUpdate.build,
                                downloadUrl = serverUpdate.url
                            )
                        )
                    } else {
                        _uiEvent.emit(ProfileUiEvent.ShowToast("当前已经是最新版本"))
                    }
                } else {
                    _uiEvent.emit(ProfileUiEvent.ShowToast(response.message ?: "检查更新失败"))
                }
            }.onFailure {
                _uiEvent.emit(ProfileUiEvent.ShowToast("网络连接异常，请稍后重试"))
            }

            _uiState.update { it.copy(isCheckingUpdate = false) }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        if (_uiState.value.isChangingPassword) return

        val oldPwd = oldPassword.trim()
        val newPwd = newPassword.trim()
        val confirmPwd = confirmPassword.trim()

        viewModelScope.launch {
            when {
                oldPwd.isEmpty() -> {
                    _uiEvent.emit(ProfileUiEvent.ShowToast("请输入当前密码"))
                    return@launch
                }

                newPwd.length < 6 -> {
                    _uiEvent.emit(ProfileUiEvent.ShowToast("新密码至少需要 6 位"))
                    return@launch
                }

                newPwd != confirmPwd -> {
                    _uiEvent.emit(ProfileUiEvent.ShowToast("两次输入的新密码不一致"))
                    return@launch
                }

                oldPwd == newPwd -> {
                    _uiEvent.emit(ProfileUiEvent.ShowToast("新密码不能与当前密码相同"))
                    return@launch
                }
            }

            _uiState.update { it.copy(isChangingPassword = true) }

            runCatching {
                apiService.changePassword(
                    ChangePasswordRequest(
                        oldPassword = oldPwd,
                        newPassword = newPwd
                    )
                )
            }.onSuccess { response ->
                if (response.code == 0) {
                    _uiEvent.emit(ProfileUiEvent.PasswordChanged)
                    _uiEvent.emit(ProfileUiEvent.ShowToast(response.message ?: "密码修改成功"))
                } else {
                    _uiEvent.emit(ProfileUiEvent.ShowToast(response.message ?: "密码修改失败"))
                }
            }.onFailure {
                _uiEvent.emit(ProfileUiEvent.ShowToast("网络连接异常，请稍后重试"))
            }

            _uiState.update { it.copy(isChangingPassword = false) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { apiService.logout() }
            JPushService.clearAlias(context)
            tokenManager.saveToken(null)
            _uiEvent.emit(ProfileUiEvent.NavigateToLogin)
        }
    }
}
