package com.example.screentime.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.screentime.data.entities.BadgeRarity
import com.example.screentime.data.entities.BadgeType
import com.example.screentime.utils.BadgeDefinitions

@Composable
fun BadgeCard(
    badgeType: BadgeType,
    isUnlocked: Boolean = true,
    modifier: Modifier = Modifier
) {
    val definition = BadgeDefinitions.getBadgeDefinition(badgeType)

    if (definition == null) return

    val rarityColor = when (definition.rarity) {
        BadgeRarity.COMMON -> Color(0xFF90EE90) // Light green
        BadgeRarity.RARE -> Color(0xFF4169E1) // Royal blue
        BadgeRarity.LEGENDARY -> Color(0xFFFFD700) // Gold
    }

    Column(
        modifier = modifier
            .background(
                if (isUnlocked)
                    MaterialTheme.colorScheme.surfaceContainer
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badge icon placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(rarityColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeType.name.take(2),
                style = MaterialTheme.typography.labelSmall,
                color = if (definition.rarity == BadgeRarity.LEGENDARY) Color.Black else Color.White
            )
        }

        Text(
            text = definition.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = definition.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 2
        )

        Text(
            text = "+${definition.pointsReward} pts",
            style = MaterialTheme.typography.labelSmall,
            color = rarityColor,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (!isUnlocked) {
            Text(
                text = "Locked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BadgeRow(
    badges: List<BadgeType>,
    unlockedBadges: List<BadgeType> = emptyList(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        badges.forEach { badge ->
            BadgeCard(
                badgeType = badge,
                isUnlocked = badge in unlockedBadges,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

