package com.example.screentime.data.dao
import androidx.room.*
import com.example.screentime.data.entities.AppLimit
import com.example.screentime.data.entities.AppUsageSession
import kotlinx.coroutines.flow.Flow
@Dao
interface AppLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppLimit(appLimit: AppLimit): Long
    @Update
    suspend fun updateAppLimit(appLimit: AppLimit): Int
    @Delete
    suspend fun deleteAppLimit(appLimit: AppLimit): Int
    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getAppLimit(packageName: String): AppLimit?
    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    fun getAllEnabledLimits(): Flow<List<AppLimit>>
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    suspend fun getAllLimitsOnce(): List<AppLimit>
    @Query("UPDATE app_limits SET usedTodayMinutes = :usedMinutes, isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateUsageAndBlockStatus(packageName: String, usedMinutes: Int, isBlocked: Boolean)

    @Query("UPDATE app_limits SET lastResetDate = :resetDate WHERE packageName = :packageName")
    suspend fun updateLastResetDate(packageName: String, resetDate: String)

    @Query("UPDATE app_limits SET usedTodayMinutes = 0, isBlocked = 0, lastResetDate = :resetDate")
    suspend fun resetDailyUsage(resetDate: String)
    @Query("SELECT * FROM app_limits WHERE isBlocked = 1")
    suspend fun getBlockedApps(): List<AppLimit>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageSession(session: AppUsageSession): Long
    @Query("SELECT SUM(durationMinutes) FROM app_usage_sessions WHERE packageName = :packageName AND date = :date")
    suspend fun getTotalUsageForAppOnDate(packageName: String, date: String): Int?
}
