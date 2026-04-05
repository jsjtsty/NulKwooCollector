package com.nulstudio.kwoocollector.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.nulstudio.kwoocollector.ui.dashboard.DashboardScreen
import com.nulstudio.kwoocollector.ui.dataexplorer.DataExplorerScreen
import com.nulstudio.kwoocollector.ui.profile.ProfileSettingsScreen

enum class BottomNavItem(val title: String, val icon: ImageVector) {
    Dashboard("工作台", Icons.Default.Home),
    DataExplorer("数据", Icons.Default.Dataset),
    Profile("我的", Icons.Default.Person)
}

@Composable
fun MainScreen(
    onLogoutSuccess: () -> Unit,
    onNavigateToFormDetail: (Int) -> Unit,
    onNavigateToFormHistory: () -> Unit,
    onNavigateToCreateEntry: (Int) -> Unit,
    onNavigateToEntryPreview: (Int, Int) -> Unit,
    onNavigateToEntryEdit: (Int, Int) -> Unit,
    dashboardRefreshSignal: Long = 0L,
    dataRefreshSignal: Long = 0L
) {
    var selectedIndex by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val items = BottomNavItem.entries.toTypedArray()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedIndex) {
                0 -> DashboardScreen(
                    onNavigateToFormDetail = onNavigateToFormDetail,
                    onNavigateToHistory = onNavigateToFormHistory,
                    refreshSignal = dashboardRefreshSignal
                )

                1 -> DataExplorerScreen(
                    onNavigateToCreateEntry = onNavigateToCreateEntry,
                    onNavigateToEntryDetail = onNavigateToEntryPreview,
                    onNavigateToEntryEdit = onNavigateToEntryEdit,
                    refreshSignal = dataRefreshSignal
                )

                2 -> ProfileSettingsScreen(onLogoutSuccess = onLogoutSuccess)
            }
        }
    }
}
