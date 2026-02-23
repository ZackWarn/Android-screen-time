package com.example.screentime.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.screentime.data.entities.BadgeType
import com.example.screentime.data.entities.EarnedBadge

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: EarnedBadge): Long

    @Query("SELECT * FROM earned_badges WHERE badgeType = :badgeType AND weekNumber = :weekNumber")
    suspend fun getBadgeInWeek(badgeType: BadgeType, weekNumber: Int): EarnedBadge?

    @Query("SELECT * FROM earned_badges WHERE weekNumber = :weekNumber ORDER BY unlockedDate DESC")
    suspend fun getBadgesForWeek(weekNumber: Int): List<EarnedBadge>

    @Query("SELECT COUNT(*) FROM earned_badges WHERE weekNumber = :weekNumber")
    suspend fun getBadgeCountForWeek(weekNumber: Int): Int

    @Delete
    suspend fun deleteBadge(badge: EarnedBadge): Int

    @Query("DELETE FROM earned_badges WHERE weekNumber = :weekNumber")
    suspend fun deleteBadgesForWeek(weekNumber: Int): Int

    @Query("SELECT * FROM earned_badges WHERE weekNumber = :weekNumber AND badgeType = :badgeType")
    suspend fun getBadgeCountByType(weekNumber: Int, badgeType: BadgeType): List<EarnedBadge>
}

