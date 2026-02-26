package com.example.screentime.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val limitMinutes: Int, // Daily limit in minutes
    val isEnabled: Boolean = true,
    val usedTodayMinutes: Int = 0, // Reset daily
    val lastResetDate: String = "", // ISO format date
    val isBlocked: Boolean = false // Set when limit is exceeded
)

@Entity(tableName = "app_usage_sessions")
data class AppUsageSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val date: String, // ISO format date
    val startTime: Long, // Unix timestamp
    val endTime: Long, // Unix timestamp
    val durationMinutes: Int
)
