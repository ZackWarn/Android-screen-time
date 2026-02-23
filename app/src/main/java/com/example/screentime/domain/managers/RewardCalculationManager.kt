package com.example.screentime.domain.managers

import com.example.screentime.data.entities.BadgeType
import com.example.screentime.data.entities.DailyProgress
import com.example.screentime.data.repository.BadgeRepository
import com.example.screentime.data.repository.ScreenTimeRepository
import com.example.screentime.utils.WeekUtils
import java.time.LocalDate

class RewardCalculationManager(
    private val badgeRepository: BadgeRepository,
    private val screenTimeRepository: ScreenTimeRepository
) {

    companion object {
        const val DAILY_SCREEN_TIME_LIMIT = 240 // 4 hours in minutes
        const val POINTS_PER_HOUR_UNDER_LIMIT = 1
        const val BONUS_ZERO_DAY = 5
        const val BONUS_PER_BADGE = 10
    }

    /**
     * Calculate points for a single day based on screen time
     */
    suspend fun calculateDailyPoints(screenTimeMinutes: Int, badgesUnlockedCount: Int): Int {
        var points = 0

        // 1 point per hour under limit (max 4 points for 4-hour limit)
        if (screenTimeMinutes < DAILY_SCREEN_TIME_LIMIT) {
            points += (screenTimeMinutes / 60) * POINTS_PER_HOUR_UNDER_LIMIT
        }

        // Bonus for zero screen time day
        if (screenTimeMinutes == 0) {
            points += BONUS_ZERO_DAY
        }

        // Points from badges unlocked
        points += badgesUnlockedCount * BONUS_PER_BADGE

        return points
    }

    /**
     * Check and unlock badges for the current day
     */
    suspend fun checkAndUnlockBadges(weekNumber: Int): List<BadgeType> {
        val unlockedBadges = mutableListOf<BadgeType>()
        val weekStart = WeekUtils.getWeekStartDate(weekNumber)
        val weekEnd = WeekUtils.getWeekEndDate(weekNumber)

        // FOCUSED_WEEK: 5+ days under 4 hours
        if (shouldUnlockFocusedWeek(weekStart, weekEnd, weekNumber)) {
            unlockedBadges.add(BadgeType.FOCUSED_WEEK)
        }

        // CONSISTENT_USER: 7 consecutive days using app
        if (shouldUnlockConsistentUser(weekStart, weekEnd, weekNumber)) {
            unlockedBadges.add(BadgeType.CONSISTENT_USER)
        }

        // IMPROVEMENT: 30% less than last week
        if (shouldUnlockImprovement(weekStart, weekEnd, weekNumber)) {
            unlockedBadges.add(BadgeType.IMPROVEMENT)
        }

        // Unlock badges in repository
        for (badge in unlockedBadges) {
            if (!badgeRepository.isBadgeUnlockedInWeek(badge, weekNumber)) {
                badgeRepository.unlockBadge(badge, weekNumber)
            }
        }

        return unlockedBadges
    }

    /**
     * Check if ZERO_DAY badge should be unlocked for today
     */
    suspend fun checkZeroDayBadge(weekNumber: Int): Boolean {
        val today = LocalDate.now()
        val progress = screenTimeRepository.getDailyProgress(today)
        return if (progress != null && progress.screenTimeMinutes == 0) {
            if (!badgeRepository.isBadgeUnlockedInWeek(BadgeType.ZERO_DAY, weekNumber)) {
                badgeRepository.unlockBadge(BadgeType.ZERO_DAY, weekNumber)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    private suspend fun shouldUnlockFocusedWeek(
        weekStart: LocalDate,
        weekEnd: LocalDate,
        weekNumber: Int
    ): Boolean {
        val dailyProgress = screenTimeRepository.getWeeklyProgress(weekStart, weekEnd)
        val daysUnderLimit = dailyProgress.count { it.screenTimeMinutes < DAILY_SCREEN_TIME_LIMIT }
        return daysUnderLimit >= 5 && !badgeRepository.isBadgeUnlockedInWeek(
            BadgeType.FOCUSED_WEEK,
            weekNumber
        )
    }

    private suspend fun shouldUnlockConsistentUser(
        weekStart: LocalDate,
        weekEnd: LocalDate,
        weekNumber: Int
    ): Boolean {
        val dailyProgress = screenTimeRepository.getWeeklyProgress(weekStart, weekEnd)
        return dailyProgress.size == 7 && !badgeRepository.isBadgeUnlockedInWeek(
            BadgeType.CONSISTENT_USER,
            weekNumber
        )
    }

    private suspend fun shouldUnlockImprovement(
        weekStart: LocalDate,
        weekEnd: LocalDate,
        weekNumber: Int
    ): Boolean {
        val currentWeekTotal = screenTimeRepository.getTotalScreenTime(weekStart, weekEnd)
        val prevWeekNumber = WeekUtils.getPreviousWeekNumber()
        val prevWeekYear = WeekUtils.getPreviousWeekYear()
        val prevWeekStart = WeekUtils.getWeekStartDate(prevWeekNumber, prevWeekYear)
        val prevWeekEnd = WeekUtils.getWeekEndDate(prevWeekNumber, prevWeekYear)
        val previousWeekTotal = screenTimeRepository.getTotalScreenTime(prevWeekStart, prevWeekEnd)

        if (previousWeekTotal == 0) return false

        val improvement = ((previousWeekTotal - currentWeekTotal).toFloat() / previousWeekTotal) * 100
        return improvement >= 30 && !badgeRepository.isBadgeUnlockedInWeek(
            BadgeType.IMPROVEMENT,
            weekNumber
        )
    }
}

