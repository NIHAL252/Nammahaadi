package com.nammahaadi.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "map",
            onClick = { onNavigate("map") },
            icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
            label = { Text("Map") }
        )
        NavigationBarItem(
            selected = currentRoute == "leaderboard",
            onClick = { onNavigate("leaderboard") },
            icon = { Icon(Icons.Default.Star, contentDescription = "Leaders") },
            label = { Text("Leaders") }
        )
        NavigationBarItem(
            selected = currentRoute == "chat",
            onClick = { onNavigate("chat") },
            icon = { Icon(Icons.Default.Info, contentDescription = "AI Help") },
            label = { Text("AI Help") }
        )
    }
}