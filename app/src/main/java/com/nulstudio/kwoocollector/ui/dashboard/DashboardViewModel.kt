package com.nulstudio.kwoocollector.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.response.FormAbstractResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val forms: List<FormAbstractResponse> = emptyList(),
    val historyForms: List<FormAbstractResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isLoadingHistory: Boolean = false,
    val isRefreshingHistory: Boolean = false,
    val historyError: String? = null
)

class DashboardViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        fetchTasks()
        fetchHistory()
    }

    fun fetchTasks(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh && it.forms.isEmpty(),
                    isRefreshing = isRefresh,
                    errorMessage = null
                )
            }

            runCatching {
                apiService.fetchForms()
            }.onSuccess { response ->
                if (response.code == 0 && response.result != null) {
                    _uiState.update {
                        it.copy(
                            forms = response.result,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = response.message ?: "服务端返回异常"
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.localizedMessage ?: "网络连接失败"
                    )
                }
            }
        }
    }

    fun fetchHistory(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingHistory = !isRefresh && it.historyForms.isEmpty(),
                    isRefreshingHistory = isRefresh,
                    historyError = null
                )
            }

            runCatching {
                apiService.fetchFormHistory()
            }.onSuccess { response ->
                if (response.code == 0 && response.result != null) {
                    _uiState.update {
                        it.copy(
                            historyForms = response.result,
                            isLoadingHistory = false,
                            isRefreshingHistory = false,
                            historyError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            isRefreshingHistory = false,
                            historyError = response.message ?: "加载历史失败"
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        isRefreshingHistory = false,
                        historyError = error.localizedMessage ?: "网络连接失败"
                    )
                }
            }
        }
    }
}
