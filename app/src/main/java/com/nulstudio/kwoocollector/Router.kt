package com.nulstudio.kwoocollector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.nulstudio.kwoocollector.ui.EntryEditorScreen
import com.nulstudio.kwoocollector.ui.EntryPreviewScreen
import com.nulstudio.kwoocollector.ui.FormTaskScreen
import com.nulstudio.kwoocollector.ui.HistoryFormPreviewScreen
import com.nulstudio.kwoocollector.ui.LoginScreen
import com.nulstudio.kwoocollector.ui.MainScreen
import com.nulstudio.kwoocollector.ui.dashboard.FormHistoryScreen
import com.nulstudio.kwoocollector.ui.splash.SplashScreen
import com.nulstudio.kwoocollector.push.PushNavigationCenter
import com.nulstudio.kwoocollector.util.TokenManager
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object Splash

@Serializable
object Main

@Serializable
object Login

@Serializable
data class FormTaskRoute(val formId: Int)

@Serializable
data class EntryEditorRoute(val tableId: Int, val entryId: Int = -1)

@Serializable
data class EntryPreviewRoute(val tableId: Int, val entryId: Int)

@Serializable
object FormHistoryRoute

@Serializable
data class FormHistoryDetailRoute(val formId: Int)

@Composable
fun AppNavigation(navController: NavHostController) {
    val tokenManager = koinInject<TokenManager>()
    val token by tokenManager.tokenFlow.collectAsState(initial = null)
    val pendingPushTarget by PushNavigationCenter.pendingTarget.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    PushNotificationPermissionEffect(token != null)

    LaunchedEffect(pendingPushTarget, token, currentDestination) {
        val target = pendingPushTarget ?: return@LaunchedEffect
        if (token.isNullOrBlank()) return@LaunchedEffect

        if (currentDestination.requiresMainRoot()) {
            navController.navigate(Main) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }

        navController.navigate(FormTaskRoute(target.formId)) {
            launchSingleTop = true
        }
        PushNavigationCenter.consume()
    }

    NavHost(
        navController = navController,
        startDestination = Splash
    ) {
        composable<Splash> {
            SplashScreen(
                onNavigate = { target ->
                    navController.navigate(target) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Main> {
            val dashboardRefreshSignal by it.savedStateHandle
                .getStateFlow("dashboard_refresh_signal", 0L)
                .collectAsState()
            val dataRefreshSignal by it.savedStateHandle
                .getStateFlow("data_refresh_signal", 0L)
                .collectAsState()

            MainScreen(
                onLogoutSuccess = {
                    navController.navigate(Login) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToFormDetail = { formId ->
                    navController.navigate(FormTaskRoute(formId))
                },
                onNavigateToFormHistory = {
                    navController.navigate(FormHistoryRoute)
                },
                onNavigateToCreateEntry = { tableId ->
                    navController.navigate(EntryEditorRoute(tableId = tableId))
                },
                onNavigateToEntryPreview = { tableId, entryId ->
                    navController.navigate(EntryPreviewRoute(tableId = tableId, entryId = entryId))
                },
                onNavigateToEntryEdit = { tableId, entryId ->
                    navController.navigate(EntryEditorRoute(tableId = tableId, entryId = entryId))
                },
                dashboardRefreshSignal = dashboardRefreshSignal,
                dataRefreshSignal = dataRefreshSignal
            )
        }

        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Main) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<FormTaskRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<FormTaskRoute>()
            FormTaskScreen(
                formId = route.formId,
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("dashboard_refresh_signal", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        composable<EntryEditorRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EntryEditorRoute>()
            EntryEditorScreen(
                tableId = route.tableId,
                entryId = route.entryId,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("data_refresh_signal", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        composable<EntryPreviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EntryPreviewRoute>()
            EntryPreviewScreen(
                tableId = route.tableId,
                entryId = route.entryId,
                onBack = { navController.popBackStack() },
                onEdit = {
                    navController.navigate(EntryEditorRoute(tableId = route.tableId, entryId = route.entryId))
                }
            )
        }

        composable<FormHistoryRoute> {
            FormHistoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHistoryDetail = { formId ->
                    navController.navigate(FormHistoryDetailRoute(formId))
                }
            )
        }

        composable<FormHistoryDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<FormHistoryDetailRoute>()
            HistoryFormPreviewScreen(
                formId = route.formId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PushNotificationPermissionEffect(isLoggedIn: Boolean) {
    if (!isLoggedIn || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun NavDestination?.requiresMainRoot(): Boolean {
    val route = this?.route.orEmpty()
    return this == null || route.contains("Splash") || route.contains("Login")
}
