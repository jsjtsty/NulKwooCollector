package com.nulstudio.kwoocollector.ui.dataexplorer

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.response.EntryAbstractResponse

class EntryPagingSource(
    private val apiService: ApiService,
    private val tableId: Int,
    private val keyword: String
) : PagingSource<Int, EntryAbstractResponse>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EntryAbstractResponse> {
        val page = params.key ?: 1
        val limit = params.loadSize

        return try {
            val response = apiService.listTable(
                tableId = tableId,
                page = page,
                limit = limit,
                keyword = keyword
            )

            if (response.code == 0 && response.result != null) {
                val items = response.result.abstracts
                LoadResult.Page(
                    data = items,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (items.isEmpty() || items.size < limit) null else page + 1
                )
            } else {
                LoadResult.Error(Exception(response.message ?: "业务接口异常"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, EntryAbstractResponse>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
