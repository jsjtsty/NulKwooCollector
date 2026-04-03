package com.nulstudio.kwoocollector.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nulstudio.kwoocollector.Login
import com.nulstudio.kwoocollector.Main
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onNavigate: (Any) -> Unit
) {
    val target by viewModel.target.collectAsState()

    LaunchedEffect(target) {
        when (target) {
            is SplashNavTarget.Main -> onNavigate(Main)
            is SplashNavTarget.Login -> onNavigate(Login)
            else -> { /* 继续展示 Loading 或 Logo */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(100.dp))
    }
}