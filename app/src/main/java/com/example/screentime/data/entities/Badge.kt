package com.example.screentime.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BadgeType {
    FOCUSED_WEEK,
    ZERO_DAY,
    CONSISTENT_USER,
    IMPROVEMENT,
    CHAMPION
}

enum class BadgeRarity {
    COMMON,
    RARE,
    LEGENDARY
}

@Entity(tableName = "badge_definitions")
data class BadgeDefinition(
    @PrimaryKey
    val type: BadgeType,
    val name: String,
    val description: String,
    val rarity: BadgeRarity,
    val pointsReward: Int
)

@Entity(tableName = "earned_badges")
data class EarnedBadge(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val badgeType: BadgeType,
    val unlockedDate: String, // ISO format date-time
    val weekNumber: Int // ISO week number
)

