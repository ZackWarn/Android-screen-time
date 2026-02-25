package com.example.screentime.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screentime.presentation.components.AppLimitSetterCard
import com.example.screentime.presentation.components.DailyProgressItem
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.screentime.presentation.components.StatCard
import com.example.screentime.presentation.components.WeeklyProgressCard
import com.example.screentime.presentation.viewmodels.DashboardViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshUsageNow()
            kotlinx.coroutines.delay(30_000)
        }
    }

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

    if (uiState.error != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error: ${uiState.error}",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    // Filter apps based on search query
    val filteredApps = remember(uiState.installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.installedApps
        } else {
            uiState.installedApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            WeeklyProgressCard(
                weekNumber = uiState.weekNumber,
                currentPoints = uiState.currentWeekPoints
            )
        }

        // App Limits Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "App Limits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Set daily time limits for your apps (${uiState.installedApps.size} apps found)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // List of apps with limit setters
        if (uiState.installedApps.isEmpty()) {
            item {
                Text(
                    text = "Loading apps...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (filteredApps.isEmpty()) {
            item {
                Text(
                    text = "No apps found matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(filteredApps) { app ->
                val currentLimit = uiState.appLimits[app.packageName]
                var pastUsageData by remember { mutableStateOf(emptyMap<String, Int>()) }
                var currentUsageMinutes by remember { mutableStateOf(0) }

                // Fetch past usage data when app is displayed
                LaunchedEffect(app.packageName) {
                    try {
                        pastUsageData = viewModel.getPastUsageForApp(app.packageName)
                        android.util.Log.d("DashboardScreen", "Fetched past usage for ${app.appName}: ${pastUsageData.size} days")
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardScreen", "Error fetching past usage", e)
                    }
                }

                // Fetch current usage minutes for minimum limit baseline
                LaunchedEffect(app.packageName) {
                    try {
                        currentUsageMinutes = viewModel.getCurrentUsageMinutes(app.packageName)
                        android.util.Log.d("DashboardScreen", "Current usage for ${app.appName}: $currentUsageMinutes min")
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardScreen", "Error fetching current usage", e)
                    }
                }

                AppLimitSetterCard(
                    appInfo = app,
                    currentLimit = currentLimit,
                    currentUsageMinutes = currentUsageMinutes,
                    onSetLimit = { minutes ->
                        viewModel.setAppLimit(app.packageName, app.appName, minutes)
                    },
                    onDeleteLimit = {
                        viewModel.deleteAppLimit(app.packageName)
                    },
                    pastUsageData = pastUsageData
                )
            }
        }

        item {
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Weekly Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        items(uiState.weeklyProgress) { progress ->
            val dayOfWeek = progress.date.dayOfWeek.getDisplayName(
                TextStyle.SHORT,
                Locale.getDefault()
            )
            DailyProgressItem(
                dayOfWeek = dayOfWeek,
                screenTimeMinutes = progress.screenTimeMinutes,
                pointsEarned = progress.pointsEarned
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Badges Earned This Week",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.earnedBadges.isEmpty()) {
                    Text(
                        text = "No badges unlocked yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "${uiState.earnedBadges.size} badge(s) unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Text("") // Spacer
        }
    }
}
