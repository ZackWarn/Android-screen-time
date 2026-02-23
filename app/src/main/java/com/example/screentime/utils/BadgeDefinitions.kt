package com.example.screentime.utils

import com.example.screentime.data.entities.BadgeDefinition
import com.example.screentime.data.entities.BadgeRarity
import com.example.screentime.data.entities.BadgeType

object BadgeDefinitions {
    fun getAllBadges(): List<BadgeDefinition> = listOf(
        BadgeDefinition(
            type = BadgeType.FOCUSED_WEEK,
            name = "Focused Week",
            description = "Keep screen time under 4 hours for 5+ days",
            rarity = BadgeRarity.COMMON,
            pointsReward = 10
        ),
        BadgeDefinition(
            type = BadgeType.ZERO_DAY,
            name = "Zero Day",
            description = "Complete 24 hours with zero screen time",
            rarity = BadgeRarity.RARE,
            pointsReward = 15
        ),
        BadgeDefinition(
            type = BadgeType.CONSISTENT_USER,
            name = "Consistent User",
            description = "Use the app every day for 7 days",
            rarity = BadgeRarity.RARE,
            pointsReward = 20
        ),
        BadgeDefinition(
            type = BadgeType.IMPROVEMENT,
            name = "Improvement",
            description = "Reduce screen time by 30% vs. previous week",
            rarity = BadgeRarity.RARE,
            pointsReward = 25
        ),
        BadgeDefinition(
            type = BadgeType.CHAMPION,
            name = "Champion",
            description = "Earn all other badges in a single week",
            rarity = BadgeRarity.LEGENDARY,
            pointsReward = 50
        )
    )

    fun getBadgeDefinition(type: BadgeType): BadgeDefinition? {
        return getAllBadges().find { it.type == type }
    }
}

