package com.example.screentime.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screentime.data.entities.BadgeType
import com.example.screentime.presentation.components.BadgeCard
import com.example.screentime.presentation.components.StatCard
import com.example.screentime.presentation.viewmodels.RewardsViewModel
import com.example.screentime.utils.BadgeDefinitions

@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel,
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

    val unlockedBadgeTypes = uiState.earnedBadges.map { it.badgeType }.toSet()
    val allBadges = BadgeDefinitions.getAllBadges().map { it.type }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Rewards",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        StatCard(
            label = "This Week's Points",
            value = "${uiState.badgeCount * 10}",
            subText = "${uiState.badgeCount} badge(s) unlocked",
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Available Badges",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allBadges.size) { index ->
                BadgeCard(
                    badgeType = allBadges[index],
                    isUnlocked = allBadges[index] in unlockedBadgeTypes
                )
            }
        }
    }
}

