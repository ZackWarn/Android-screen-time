package com.example.screentime.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_stats")
data class WeeklyStats(
    @PrimaryKey
    val weekNumber: Int, // ISO week number
    val year: Int,
    val totalScreenTimeMinutes: Int = 0,
    val totalPointsEarned: Int = 0,
    val totalBadgesEarned: Int = 0,
    val averageDailyUsageMinutes: Float = 0f,
    val isCompleted: Boolean = false // true after week is finalized
)

