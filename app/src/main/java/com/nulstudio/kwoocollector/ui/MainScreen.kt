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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(val title: String, val icon: ImageVector) {
    Dashboard("工作台", Icons.Default.Home),
    DataExplorer("数据", Icons.Default.Dataset),
    Profile("我的", Icons.Default.Person)
}

@Composable
fun MainScreen() {
    // 记录当前选中的 Tab 索引
    var selectedIndex by remember { mutableIntStateOf(0) }
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
        // 2. 根据选中的索引，切换显示不同的页面
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedIndex) {
                0 -> DashboardScreen()         // 我们之前写的工作台
                1 -> DataExplorerScreen()      // 数据管理页（需要新建）
                2 -> ProfileSettingsScreen()   // 我们之前写的个人中心
            }
        }
    }
}