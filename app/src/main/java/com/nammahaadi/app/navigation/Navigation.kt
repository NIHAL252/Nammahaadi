package com.nammahaadi.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nammahaadi.app.ui.screens.map.MapScreen
import com.nammahaadi.app.ui.screens.leaderboard.LeaderboardScreen
import com.nammahaadi.app.ui.screens.gemini.GeminiChatScreen

sealed class Screen(val route: String, val label: String) {
    object Map         : Screen("map", "Map")
    object Leaderboard : Screen("leaderboard", "Leaders")
    object Chat        : Screen("chat", "AI Help")
}

@Composable
fun NammaHaadiApp() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Map, Screen.Leaderboard, Screen.Chat)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute by navController.currentBackStackEntryAsState()
                val route = currentRoute?.destination?.route
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.Map -> Icon(Icons.Default.Place, contentDescription = "Map")
                                Screen.Leaderboard -> Icon(Icons.Default.Star, contentDescription = "Leaderboard")
                                Screen.Chat -> Icon(Icons.Default.Info, contentDescription = "Chat")
                            }
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route
        ) {
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Leaderboard.route) { LeaderboardScreen() }
            composable(Screen.Chat.route) { GeminiChatScreen() }
        }
    }
}