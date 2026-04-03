package com.nulstudio.kwoocollector.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.util.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class SplashNavTarget {
    object Loading : SplashNavTarget()
    object Login : SplashNavTarget()
    object Main : SplashNavTarget()
}

class SplashViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _target = MutableStateFlow<SplashNavTarget>(SplashNavTarget.Loading)
    val target: StateFlow<SplashNavTarget> = _target

    init {
        verifyToken()
    }

    private fun verifyToken() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            val token = tokenManager.tokenFlow.firstOrNull()

            if (token.isNullOrBlank()) {
                _target.value = SplashNavTarget.Login
            } else {
                try {
                    val response = apiService.verifyToken()
                    if (response.isSuccessful) {
                        _target.value = SplashNavTarget.Main
                    } else {
                        tokenManager.saveToken(null)
                        _target.value = SplashNavTarget.Login
                    }
                } catch (_: Exception) {
                    _target.value = SplashNavTarget.Main
                }
            }

            val duration = System.currentTimeMillis() - startTime
            if (duration < 800) delay(800 - duration)
        }
    }
}