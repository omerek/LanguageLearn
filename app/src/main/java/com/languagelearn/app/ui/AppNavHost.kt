package com.languagelearn.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.languagelearn.app.AppContainer
import com.languagelearn.app.ui.home.HomeScreen
import com.languagelearn.app.ui.placement.PlacementScreen
import com.languagelearn.app.ui.progress.ProgressScreen
import com.languagelearn.app.ui.session.SessionScreen

object Routes {
    const val PLACEMENT = "placement"
    const val HOME = "home"
    const val SESSION = "session"
    const val PROGRESS = "progress"
}

@Composable
fun AppNavHost(container: AppContainer) {
    // null = still loading; decides the start screen once.
    val start by produceState<String?>(initialValue = null) {
        value = if (container.learningRepository.getProfile()?.placementDone == true) Routes.HOME else Routes.PLACEMENT
    }
    val startRoute = start
    if (startRoute == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startRoute) {
        composable(Routes.PLACEMENT) {
            PlacementScreen(container) {
                nav.navigate(Routes.HOME) { popUpTo(Routes.PLACEMENT) { inclusive = true } }
            }
        }
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onStartSession = { nav.navigate(Routes.SESSION) },
                onOpenProgress = { nav.navigate(Routes.PROGRESS) },
            )
        }
        composable(Routes.SESSION) {
            SessionScreen(container = container, onExit = { nav.popBackStack() })
        }
        composable(Routes.PROGRESS) {
            ProgressScreen(container = container, onBack = { nav.popBackStack() })
        }
    }
}
