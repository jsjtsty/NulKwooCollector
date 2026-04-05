package com.nulstudio.kwoocollector.ui.dataexplorer

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.response.EntryAbstractResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExplorerScreen(
    viewModel: DataExplorerViewModel = koinViewModel(),
    onNavigateToCreateEntry: (tableId: Int) -> Unit,
    onNavigateToEntryDetail: (tableId: Int, entryId: Int) -> Unit,
    onNavigateToEntryEdit: (tableId: Int, entryId: Int) -> Unit,
    refreshSignal: Long = 0L
) {
    val apiService = koinInject<ApiService>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) pendingDelete = null
            },
            title = { Text("删除记录") },
            text = { Text("删除后不可恢复，确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val (tableId, entryId) = pendingDelete ?: return@TextButton
                        scope.launch {
                            isDeleting = true
                            runCatching { apiService.deleteEntry(tableId, entryId) }
                                .onSuccess { response ->
                                    if (response.code == 0) {
                                        Toast.makeText(context, "记录已删除", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            response.message ?: "删除失败",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.localizedMessage ?: "网络连接异常",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            isDeleting = false
                            pendingDelete = null
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text(if (isDeleting) "删除中..." else "删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDelete = null },
                    enabled = !isDeleting
                ) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据中心", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (uiState.tables.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val currentTableId = uiState.tables[uiState.selectedTableIndex].id
                        onNavigateToCreateEntry(currentTableId)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加记录")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoadingTables) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (uiState.tables.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTableIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    uiState.tables.forEachIndexed { index, table ->
                        Tab(
                            selected = uiState.selectedTableIndex == index,
                            onClick = { viewModel.selectTab(index) },
                            text = {
                                Text(
                                    text = table.name,
                                    fontWeight = if (uiState.selectedTableIndex == index) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        )
                    }
                }

                val currentCategoryName =
                    uiState.tables.getOrNull(uiState.selectedTableIndex)?.name ?: "当前分类"
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("在$currentCategoryName 中搜索...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                val currentTableId = uiState.tables[uiState.selectedTableIndex].id
                val appliedQuery = uiState.searchQuery.trim()
                val pagingDataFlow = remember(currentTableId, appliedQuery) {
                    viewModel.getPagingData(currentTableId, appliedQuery)
                }
                val lazyPagingItems = pagingDataFlow.collectAsLazyPagingItems()
                var isPullRefreshing by remember(currentTableId) { mutableStateOf(false) }

                LaunchedEffect(refreshSignal, currentTableId) {
                    if (refreshSignal != 0L) {
                        lazyPagingItems.refresh()
                    }
                }

                LaunchedEffect(pendingDelete, currentTableId) {
                    if (pendingDelete == null && !isDeleting) {
                        lazyPagingItems.refresh()
                    }
                }

                LaunchedEffect(currentTableId, appliedQuery) {
                    if (appliedQuery.isNotEmpty()) {
                        delay(250)
                    }
                    lazyPagingItems.refresh()
                }

                LaunchedEffect(lazyPagingItems.loadState.refresh) {
                    if (lazyPagingItems.loadState.refresh !is LoadState.Loading) {
                        isPullRefreshing = false
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isPullRefreshing,
                    onRefresh = {
                        isPullRefreshing = true
                        lazyPagingItems.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey { it.id }
                        ) { index ->
                            val entry = lazyPagingItems[index]
                            if (entry != null) {
                                DataRecordCard(
                                    entry = entry,
                                    onClick = { onNavigateToEntryDetail(currentTableId, entry.id) },
                                    onEdit = { onNavigateToEntryEdit(currentTableId, entry.id) },
                                    onDelete = { pendingDelete = currentTableId to entry.id }
                                )
                            }
                        }

                        when (lazyPagingItems.loadState.append) {
                            is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }

                            is LoadState.Error -> {
                                item {
                                    TextButton(
                                        onClick = { lazyPagingItems.retry() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("加载失败，点击重试")
                                    }
                                }
                            }

                            is LoadState.NotLoading -> {
                                if (lazyPagingItems.itemCount == 0) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (appliedQuery.isBlank()) "暂无数据" else "没有找到匹配结果",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataRecordCard(
    entry: EntryAbstractResponse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${entry.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val sec1 = entry.secondary.getOrNull(0) ?: ""
                val sec2 = entry.secondary.getOrNull(1) ?: ""

                Text(
                    text = sec1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sec2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
