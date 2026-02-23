package com.example.screentime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.screentime.data.entities.WeeklyStats

@Dao
interface WeeklyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: WeeklyStats): Long

    @Query("SELECT * FROM weekly_stats WHERE weekNumber = :weekNumber AND year = :year")
    suspend fun getStatsByWeek(weekNumber: Int, year: Int): WeeklyStats?

    @Query("SELECT * FROM weekly_stats WHERE weekNumber IN (:weekNumbers) AND year = :year ORDER BY weekNumber DESC")
    suspend fun getStatsByWeeks(weekNumbers: List<Int>, year: Int): List<WeeklyStats>

    @Update
    suspend fun updateStats(stats: WeeklyStats): Int

    @Query("DELETE FROM weekly_stats WHERE weekNumber = :weekNumber AND year = :year")
    suspend fun deleteStatsByWeek(weekNumber: Int, year: Int): Int

    @Query("SELECT * FROM weekly_stats WHERE isCompleted = 1 ORDER BY year DESC, weekNumber DESC LIMIT 1")
    suspend fun getLastCompletedWeek(): WeeklyStats?
}

