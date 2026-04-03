package com.nulstudio.kwoocollector

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nulstudio.kwoocollector.ui.LoginScreen
import com.nulstudio.kwoocollector.ui.MainScreen
import com.nulstudio.kwoocollector.ui.splash.SplashScreen
import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object Main

@Serializable
object Login

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Splash
    ) {
        composable<Splash> {
            SplashScreen(
                onNavigate = { target ->
                    navController.navigate(target)
                }
            )
        }

        composable<Main> {
            MainScreen()
        }

        composable<Login> {
            LoginScreen()
        }
    }
}