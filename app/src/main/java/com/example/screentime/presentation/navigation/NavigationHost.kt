package com.example.screentime.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.screentime.data.ScreenTimeDatabase
import com.example.screentime.data.repository.BadgeRepository
import com.example.screentime.data.repository.ScreenTimeRepository
import com.example.screentime.data.repository.WeeklyStatsRepository
import com.example.screentime.presentation.screens.AnalyticsScreen
import com.example.screentime.presentation.screens.DashboardScreen
import com.example.screentime.presentation.screens.RewardsScreen
import com.example.screentime.presentation.viewmodels.AnalyticsViewModel
import com.example.screentime.presentation.viewmodels.DashboardViewModel
import com.example.screentime.presentation.viewmodels.RewardsViewModel
import android.content.Context

sealed class Screen(val route: String, val label: String) {
    data object Dashboard : Screen("dashboard", "Home")
    data object Analytics : Screen("analytics", "Analytics")
    data object Rewards : Screen("rewards", "Rewards")
}

@Composable
fun NavigationHost(
    context: Context,
    modifier: Modifier = Modifier
) {
    val selectedTab = remember { mutableIntStateOf(0) }

    // Initialize database and repositories
    val db = ScreenTimeDatabase.getDatabase(context)
    val screenTimeRepository = ScreenTimeRepository(db.dailyProgressDao())
    val badgeRepository = BadgeRepository(db.badgeDao())
    val weeklyStatsRepository = WeeklyStatsRepository(db.weeklyStatsDao())

    // Initialize ViewModels
    val dashboardViewModel = remember {
        DashboardViewModel(screenTimeRepository, badgeRepository, weeklyStatsRepository)
    }
    val analyticsViewModel = remember {
        AnalyticsViewModel(screenTimeRepository, weeklyStatsRepository)
    }
    val rewardsViewModel = remember {
        RewardsViewModel(badgeRepository)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    selected = selectedTab.intValue == 0,
                    onClick = { selectedTab.intValue = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    selected = selectedTab.intValue == 1,
                    onClick = { selectedTab.intValue = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Rewards") },
                    label = { Text("Rewards") },
                    selected = selectedTab.intValue == 2,
                    onClick = { selectedTab.intValue = 2 }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (selectedTab.intValue) {
            0 -> DashboardScreen(
                viewModel = dashboardViewModel,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> AnalyticsScreen(
                viewModel = analyticsViewModel,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> RewardsScreen(
                viewModel = rewardsViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}



