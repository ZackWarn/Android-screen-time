package com.example.screentime.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.screentime.data.entities.AppUsageSession

@Dao
interface AppUsageSessionDao {

    @Insert
    suspend fun insertSession(session: AppUsageSession)

    @Update
    suspend fun updateSession(session: AppUsageSession)

    @Delete
    suspend fun deleteSession(session: AppUsageSession)

    @Query("SELECT * FROM app_usage_sessions WHERE packageName = :packageName")
    suspend fun getSessionsForPackage(packageName: String): List<AppUsageSession>

    @Query("SELECT * FROM app_usage_sessions WHERE date = :date AND packageName = :packageName")
    suspend fun getSessionsForPackageOnDate(packageName: String, date: String): List<AppUsageSession>

    @Query("SELECT SUM(durationMinutes) FROM app_usage_sessions WHERE packageName = :packageName AND date = :date")
    suspend fun getTotalMinutesForPackageOnDate(packageName: String, date: String): Int?

    @Query("DELETE FROM app_usage_sessions WHERE date < :beforeDate")
    suspend fun deleteSessionsBefore(beforeDate: String)

    @Query("SELECT * FROM app_usage_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<AppUsageSession>
}

