package com.nammahaadi.app.ui.screens.leaderboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nammahaadi.app.data.model.Contributor
import com.nammahaadi.app.viewmodel.LeaderboardViewModel

@Composable
fun LeaderboardScreen(vm: LeaderboardViewModel = viewModel()) {

    val contributorList: List<Contributor> by vm.leaderboard.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()        // ✅ FIX: header no longer clips under status bar
            .navigationBarsPadding()    // ✅ FIX: content clears bottom nav bar
            .padding(16.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Text(
            text = "🏆 Top Contributors",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Villagers keeping the map accurate",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── Body ────────────────────────────────────────────────────────────
        if (contributorList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌾",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No contributors yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Be the first to map a path!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = contributorList,
                    key = { _, item -> item.userId }
                ) { index, contributor ->
                    val medal = when (index) {
                        0    -> "🥇"
                        1    -> "🥈"
                        2    -> "🥉"
                        else -> "${index + 1}."
                    }
                    ContributorCard(
                        medal = medal,
                        contributor = contributor
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributorCard(
    medal: String,
    contributor: Contributor
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = medal,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.width(40.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contributor.name.ifBlank { "Anonymous" },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${contributor.pathsAdded} paths · ${contributor.reportsCount} reports",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "${contributor.totalPoints} pts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }
    }
}