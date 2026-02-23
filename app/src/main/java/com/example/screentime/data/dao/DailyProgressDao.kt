package com.example.screentime.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.screentime.data.entities.DailyProgress
import java.time.LocalDate

@Dao
interface DailyProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DailyProgress): Long

    @Query("SELECT * FROM daily_progress WHERE date = :date")
    suspend fun getProgressByDate(date: LocalDate): DailyProgress?

    @Query("SELECT * FROM daily_progress WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getProgressRange(startDate: LocalDate, endDate: LocalDate): List<DailyProgress>

    @Update
    suspend fun updateProgress(progress: DailyProgress): Int

    @Delete
    suspend fun deleteProgress(progress: DailyProgress): Int

    @Query("DELETE FROM daily_progress WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteProgressRange(startDate: LocalDate, endDate: LocalDate): Int

    @Query("SELECT SUM(screenTimeMinutes) FROM daily_progress WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalScreenTimeBetween(startDate: LocalDate, endDate: LocalDate): Int?

    @Query("SELECT AVG(screenTimeMinutes) FROM daily_progress WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageScreenTimeBetween(startDate: LocalDate, endDate: LocalDate): Float?
}

