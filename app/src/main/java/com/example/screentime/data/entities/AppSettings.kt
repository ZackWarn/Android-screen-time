package com.example.screentime.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val dailyScreenTimeLimit: Int = 240, // minutes (4 hours default)
    val lastResetTimestamp: String = "", // ISO format
    val currentWeekNumber: Int = 1,
    val currentYear: Int = 2026
)

