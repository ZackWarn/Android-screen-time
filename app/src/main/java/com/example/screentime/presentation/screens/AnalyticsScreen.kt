package com.example.screentime.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screentime.presentation.components.StatCard
import com.example.screentime.presentation.viewmodels.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value

    if (uiState.isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Week Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val currentScreenTime = uiState.currentWeekProgress.sumOf { it.screenTimeMinutes }
                val prevScreenTime = uiState.previousWeekProgress.sumOf { it.screenTimeMinutes }

                val currentHours = currentScreenTime / 60
                val currentMinutes = currentScreenTime % 60
                val prevHours = prevScreenTime / 60
                val prevMinutes = prevScreenTime % 60

                StatCard(
                    label = "This Week",
                    value = String.format("%dh %02dm", currentHours, currentMinutes),
                    modifier = Modifier.fillMaxWidth()
                )

                StatCard(
                    label = "Last Week",
                    value = String.format("%dh %02dm", prevHours, prevMinutes),
                    modifier = Modifier.fillMaxWidth()
                )

                val improvementText = if (uiState.improvementPercentage > 0) {
                    "↓ ${String.format("%.1f", uiState.improvementPercentage)}% improvement"
                } else if (uiState.improvementPercentage < 0) {
                    "↑ ${String.format("%.1f", -uiState.improvementPercentage)}% increase"
                } else {
                    "No change"
                }

                StatCard(
                    label = "Progress",
                    value = improvementText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Daily Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "This Week (${uiState.currentWeekProgress.size} days)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                uiState.currentWeekProgress.forEach { progress ->
                    val hours = progress.screenTimeMinutes / 60
                    val minutes = progress.screenTimeMinutes % 60
                    StatCard(
                        label = progress.date.toString(),
                        value = String.format("%dh %02dm", hours, minutes),
                        subText = "+${progress.pointsEarned} pts",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Last Week (${uiState.previousWeekProgress.size} days)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                uiState.previousWeekProgress.forEach { progress ->
                    val hours = progress.screenTimeMinutes / 60
                    val minutes = progress.screenTimeMinutes % 60
                    StatCard(
                        label = progress.date.toString(),
                        value = String.format("%dh %02dm", hours, minutes),
                        subText = "+${progress.pointsEarned} pts",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Text("") // Spacer
        }
    }
}

