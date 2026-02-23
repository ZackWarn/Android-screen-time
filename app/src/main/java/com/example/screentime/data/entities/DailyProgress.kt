package com.example.screentime.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_progress")
data class DailyProgress(
    @PrimaryKey
    val date: LocalDate,
    val screenTimeMinutes: Int = 0,
    val pointsEarned: Int = 0,
    val badgesUnlockedCount: Int = 0
)

