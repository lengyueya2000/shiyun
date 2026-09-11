package com.shiyun.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shiyun.app.ui.favorites.FavoritesScreen
import com.shiyun.app.ui.home.HomeScreen
import com.shiyun.app.ui.library.LibraryScreen
import com.shiyun.app.ui.quiz.QuizScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val FAVORITES = "favorites"
    const val QUIZ = "quiz"
    const val DETAIL = "poem/{poemId}"
    fun detail(poemId: Long) = "poem/$poemId"
}

private data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun ShiyunNavHost() {
    val navController = rememberNavController()
    val tabs = listOf(
        Tab(Routes.HOME, "今日") { Icon(Icons.Default.Home, null) },
        Tab(Routes.LIBRARY, "诗词库") { Icon(Icons.Default.MenuBook, null) },
        Tab(Routes.FAVORITES, "收藏") { Icon(Icons.Default.Favorite, null) },
        Tab(Routes.QUIZ, "闯关") { Icon(Icons.Default.SportsEsports, null) },
    )
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val showBar = tabs.any { currentDestination?.hierarchy?.any { d -> d.route == it.route } == true }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(onOpenPoem = { navController.navigate(Routes.detail(it)) }) }
            composable(Routes.LIBRARY) { LibraryScreen(onOpenPoem = { navController.navigate(Routes.detail(it)) }) }
            composable(Routes.FAVORITES) { FavoritesScreen(onOpenPoem = { navController.navigate(Routes.detail(it)) }) }
            composable(Routes.QUIZ) { QuizScreen() }
            composable(Routes.DETAIL) { backStackEntry ->
                val poemId = backStackEntry.arguments?.getString("poemId")?.toLongOrNull() ?: 0L
                com.shiyun.app.ui.detail.DetailScreen(poemId = poemId, onBack = { navController.popBackStack() })
            }
        }
    }
}
