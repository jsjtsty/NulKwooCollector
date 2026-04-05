package com.nulstudio.kwoocollector.ui.dataexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.response.EntryAbstractResponse
import com.nulstudio.kwoocollector.net.model.response.TableAbstractResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataExplorerUiState(
    val isLoadingTables: Boolean = true,
    val tables: List<TableAbstractResponse> = emptyList(),
    val selectedTableIndex: Int = 0,
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class DataExplorerViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataExplorerUiState())
    val uiState: StateFlow<DataExplorerUiState> = _uiState.asStateFlow()

    private val pagers = mutableMapOf<Pair<Int, String>, Flow<PagingData<EntryAbstractResponse>>>()

    init {
        fetchTables()
    }

    fun fetchTables() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTables = true, errorMessage = null) }
            runCatching {
                apiService.fetchTables()
            }.onSuccess { response ->
                if (response.code == 0 && response.result != null) {
                    _uiState.update {
                        it.copy(isLoadingTables = false, tables = response.result, selectedTableIndex = 0)
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoadingTables = false, errorMessage = response.message ?: "获取业务表失败")
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingTables = false, errorMessage = "获取业务表失败") }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTableIndex = index) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getPagingData(tableId: Int, keyword: String): Flow<PagingData<EntryAbstractResponse>> {
        val normalizedKeyword = keyword.trim()
        val key = tableId to normalizedKeyword

        return pagers.getOrPut(key) {
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { EntryPagingSource(apiService, tableId, normalizedKeyword) }
            ).flow.cachedIn(viewModelScope)
        }
    }
}
