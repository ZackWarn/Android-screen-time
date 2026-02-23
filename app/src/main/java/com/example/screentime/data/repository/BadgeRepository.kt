package com.example.screentime.data.repository

import com.example.screentime.data.dao.BadgeDao
import com.example.screentime.data.entities.BadgeType
import com.example.screentime.data.entities.EarnedBadge
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BadgeRepository(private val badgeDao: BadgeDao) {

    suspend fun unlockBadge(badgeType: BadgeType, weekNumber: Int) {
        val badge = EarnedBadge(
            badgeType = badgeType,
            unlockedDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            weekNumber = weekNumber
        )
        badgeDao.insertBadge(badge)
    }

    suspend fun getBadgesForWeek(weekNumber: Int): List<EarnedBadge> {
        return badgeDao.getBadgesForWeek(weekNumber)
    }

    suspend fun getBadgeCountForWeek(weekNumber: Int): Int {
        return badgeDao.getBadgeCountForWeek(weekNumber)
    }

    suspend fun isBadgeUnlockedInWeek(badgeType: BadgeType, weekNumber: Int): Boolean {
        return badgeDao.getBadgeInWeek(badgeType, weekNumber) != null
    }

    suspend fun getCountOfBadgeType(weekNumber: Int, badgeType: BadgeType): Int {
        return badgeDao.getBadgeCountByType(weekNumber, badgeType).size
    }

    suspend fun clearWeekBadges(weekNumber: Int) {
        badgeDao.deleteBadgesForWeek(weekNumber)
    }
}

