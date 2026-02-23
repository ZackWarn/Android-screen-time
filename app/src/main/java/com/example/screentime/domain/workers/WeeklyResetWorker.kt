package com.example.screentime.domain.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screentime.data.ScreenTimeDatabase
import com.example.screentime.data.entities.WeeklyStats
import com.example.screentime.data.repository.ScreenTimeRepository
import com.example.screentime.data.repository.WeeklyStatsRepository
import com.example.screentime.data.repository.BadgeRepository
import com.example.screentime.utils.WeekUtils
import java.time.LocalDate

class WeeklyResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = ScreenTimeDatabase.getDatabase(applicationContext)
            val screenTimeRepository = ScreenTimeRepository(db.dailyProgressDao())
            val weeklyStatsRepository = WeeklyStatsRepository(db.weeklyStatsDao())
            val badgeRepository = BadgeRepository(db.badgeDao())

            val currentWeekNumber = WeekUtils.getCurrentWeekNumber()
            val currentYear = WeekUtils.getCurrentYear()
            val prevWeekNumber = WeekUtils.getPreviousWeekNumber()
            val prevWeekYear = WeekUtils.getPreviousWeekYear()

            // Get the previous week dates
            val prevWeekStart = WeekUtils.getWeekStartDate(prevWeekNumber, prevWeekYear)
            val prevWeekEnd = WeekUtils.getWeekEndDate(prevWeekNumber, prevWeekYear)

            // Calculate stats for the completed week
            val totalScreenTime = screenTimeRepository.getTotalScreenTime(prevWeekStart, prevWeekEnd)
            val dailyProgress = screenTimeRepository.getWeeklyProgress(prevWeekStart, prevWeekEnd)
            val totalPoints = dailyProgress.sumOf { it.pointsEarned }
            val totalBadges = badgeRepository.getBadgeCountForWeek(prevWeekNumber)
            val avgScreenTime = if (dailyProgress.isNotEmpty()) {
                dailyProgress.map { it.screenTimeMinutes }.average().toFloat()
            } else {
                0f
            }

            // Save the completed week stats
            val weeklyStats = WeeklyStats(
                weekNumber = prevWeekNumber,
                year = prevWeekYear,
                totalScreenTimeMinutes = totalScreenTime,
                totalPointsEarned = totalPoints,
                totalBadgesEarned = totalBadges,
                averageDailyUsageMinutes = avgScreenTime,
                isCompleted = true
            )

            weeklyStatsRepository.insertStats(weeklyStats)

            // Create fresh stats for the current week
            val currentWeekStats = WeeklyStats(
                weekNumber = currentWeekNumber,
                year = currentYear
            )
            weeklyStatsRepository.insertStats(currentWeekStats)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

