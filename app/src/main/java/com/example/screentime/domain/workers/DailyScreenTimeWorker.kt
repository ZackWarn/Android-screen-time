package com.example.screentime.domain.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screentime.data.ScreenTimeDatabase
import com.example.screentime.data.entities.DailyProgress
import com.example.screentime.data.repository.ScreenTimeRepository
import com.example.screentime.domain.managers.RewardCalculationManager
import com.example.screentime.domain.managers.ScreenTimeTrackerManager
import com.example.screentime.data.repository.BadgeRepository
import com.example.screentime.utils.WeekUtils
import java.time.LocalDate

class DailyScreenTimeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = ScreenTimeDatabase.getDatabase(applicationContext)
            val screenTimeRepository = ScreenTimeRepository(db.dailyProgressDao())
            val badgeRepository = BadgeRepository(db.badgeDao())
            val trackerManager = ScreenTimeTrackerManager(applicationContext)
            val rewardManager = RewardCalculationManager(badgeRepository, screenTimeRepository)

            val today = LocalDate.now()
            val screenTimeMinutes = trackerManager.getDailyScreenTime()

            // Check and unlock badges
            val weekNumber = WeekUtils.getCurrentWeekNumber()
            val unlockedBadges = rewardManager.checkAndUnlockBadges(weekNumber)
            val zeroDayUnlocked = rewardManager.checkZeroDayBadge(weekNumber)

            val badgeCount = unlockedBadges.size + if (zeroDayUnlocked) 1 else 0

            // Calculate points for today
            val pointsEarned = rewardManager.calculateDailyPoints(screenTimeMinutes, badgeCount)

            // Save daily progress
            val progress = DailyProgress(
                date = today,
                screenTimeMinutes = screenTimeMinutes,
                pointsEarned = pointsEarned,
                badgesUnlockedCount = badgeCount
            )

            screenTimeRepository.addDailyProgress(progress)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

