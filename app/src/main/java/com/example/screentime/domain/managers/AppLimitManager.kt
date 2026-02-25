package com.example.screentime.domain.managers
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.screentime.data.ScreenTimeDatabase
import com.example.screentime.data.dao.AppLimitDao
import com.example.screentime.data.entities.AppLimit
import com.example.screentime.data.entities.AppUsageSession
import com.example.screentime.domain.models.LimitStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
class AppLimitManager(
    private val context: Context,
    private val appLimitDao: AppLimitDao,
    private val database: ScreenTimeDatabase? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    /**
     * Get all apps installed on device with their usage
     */
    fun getInstalledApps(): List<AppInfo> {
        android.util.Log.d("AppLimitManager", "getInstalledApps called")
        val packageManager = context.packageManager
        val apps = mutableListOf<AppInfo>()

        // Get apps with launcher intent (apps with icons that user can launch)
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launcherApps = packageManager.queryIntentActivities(mainIntent, 0)
        android.util.Log.d("AppLimitManager", "Launcher apps found: ${launcherApps.size}")

        for (resolveInfo in launcherApps) {
            try {
                val packageName = resolveInfo.activityInfo.packageName

                // Skip our own app
                if (packageName == context.packageName) {
                    continue
                }

                val appName = resolveInfo.loadLabel(packageManager).toString()
                val appIcon = resolveInfo.loadIcon(packageManager)

                apps.add(AppInfo(packageName, appName, appIcon))
                android.util.Log.d("AppLimitManager", "Added app: $appName ($packageName)")
            } catch (e: Exception) {
                android.util.Log.e("AppLimitManager", "Error loading app: ${e.message}")
            }
        }

        android.util.Log.d("AppLimitManager", "Total accessible apps found: ${apps.size}")
        return apps.sortedBy { it.appName }
    }
    /**
     * Set limit for an app
     */
    suspend fun setAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        val today = LocalDate.now().toString()
        val usedMinutes = getCurrentAppUsageMinutes(packageName)
        val isBlocked = usedMinutes >= limitMinutes

        val appLimit = AppLimit(
            packageName = packageName,
            appName = appName,
            limitMinutes = limitMinutes,
            isEnabled = true,
            usedTodayMinutes = usedMinutes,
            lastResetDate = today,
            isBlocked = isBlocked
        )
        appLimitDao.insertAppLimit(appLimit)
    }
    /**
     * Get all app limits
     */
    fun getAllLimits(): Flow<List<AppLimit>> {
        return appLimitDao.getAllLimits()
    }
    /**
     * Update app limit
     */
    suspend fun updateAppLimit(appLimit: AppLimit) {
        appLimitDao.updateAppLimit(appLimit)
        // Send broadcast to service to clear blocked status for this app
        val intent = Intent("com.example.screentime.LIMIT_CHANGED").apply {
            putExtra("packageName", appLimit.packageName)
        }
        context.sendBroadcast(intent)
    }
    /**
     * Delete app limit
     */
    suspend fun deleteAppLimit(appLimit: AppLimit) {
        appLimitDao.deleteAppLimit(appLimit)
    }
    /**
     * Check if app has exceeded its limit
     */
    suspend fun checkAppUsage(packageName: String): LimitStatus {
        android.util.Log.d("AppLimitManager", "Checking usage for: $packageName")

        val appLimit = appLimitDao.getAppLimit(packageName)
        if (appLimit == null) {
            android.util.Log.d("AppLimitManager", "No limit found for: $packageName")
            return LimitStatus.NoLimit
        }

        android.util.Log.d("AppLimitManager", "Limit found: ${appLimit.limitMinutes} min, enabled: ${appLimit.isEnabled}")

        if (!appLimit.isEnabled) {
            android.util.Log.d("AppLimitManager", "Limit disabled for: $packageName")
            return LimitStatus.NoLimit
        }

        // Reset if new day
        val today = LocalDate.now().toString()
        if (appLimit.lastResetDate != today) {
            android.util.Log.d("AppLimitManager", "Resetting usage for new day: $packageName")
            appLimitDao.updateUsageAndBlockStatus(packageName, 0, false)
            return LimitStatus.WithinLimit(0, appLimit.limitMinutes)
        }

        // Get current usage with in-progress session time
        val currentUsageMillis = getCurrentAppUsageMillis(packageName)
        val currentUsageMinutes = (currentUsageMillis / 60000L).toInt()
        android.util.Log.d(
            "AppLimitManager",
            "Current usage for $packageName: $currentUsageMinutes min (limit: ${appLimit.limitMinutes} min)"
        )

        // Update usage
        val isBlocked = currentUsageMillis >= appLimit.limitMinutes * 60L * 1000L
        appLimitDao.updateUsageAndBlockStatus(packageName, currentUsageMinutes, isBlocked)

        return if (isBlocked) {
            android.util.Log.d("AppLimitManager", "BLOCKED: $packageName exceeded limit")
            LimitStatus.Exceeded(currentUsageMinutes, appLimit.limitMinutes)
        } else {
            android.util.Log.d("AppLimitManager", "OK: $packageName within limit")
            LimitStatus.WithinLimit(currentUsageMinutes, appLimit.limitMinutes)
        }
    }
    /**
     * Get current app usage for today in minutes
     */
    private fun getCurrentAppUsageMinutes(packageName: String): Int {
        return (getCurrentAppUsageMillis(packageName) / 60000L).toInt()
    }

    /**
     * Get current app usage for today in milliseconds.
     * Adds in-progress session time so usage updates while app is still open.
     */
    private fun getCurrentAppUsageMillis(packageName: String): Long {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val startOfDay = LocalDate.now().atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            android.util.Log.d("AppLimitManager", "Getting usage from $startOfDay to $now")

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                now
            )

            android.util.Log.d("AppLimitManager", "Total usage stats entries: ${usageStats.size}")

            val appUsage = usageStats.find { it.packageName == packageName }
            val baseUsageMillis = appUsage?.totalTimeInForeground ?: 0L

            // Add ongoing session time by checking the latest resume/pause events
            // Always query fresh from UsageEvents - don't rely on in-memory state
            val usageEvents = usageStatsManager.queryEvents(startOfDay, now)
            var latestResumeTime = 0L
            var latestPauseTime = 0L
            var lastEventType = 0  // Track the type of last event
            val event = android.app.usage.UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.packageName == packageName) {
                    when (event.eventType) {
                        android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                            latestResumeTime = event.timeStamp
                            lastEventType = event.eventType
                        }
                        android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                            latestPauseTime = event.timeStamp
                            lastEventType = event.eventType
                        }
                    }
                }
            }

            // Calculate live usage: only count time if app is currently active (last event was RESUMED)
            val inProgressMillis = if (lastEventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED && latestResumeTime > latestPauseTime) {
                (now - latestResumeTime).coerceAtLeast(0L)
            } else {
                0L
            }

            val totalUsageMillis = baseUsageMillis + inProgressMillis
            val usageMinutes = (totalUsageMillis / 1000 / 60).toInt()

            android.util.Log.d(
                "AppLimitManager",
                "Usage for $packageName: $totalUsageMillis ms = $usageMinutes min (base: $baseUsageMillis ms, live: $inProgressMillis ms)"
            )

            return totalUsageMillis
        } catch (e: Exception) {
            android.util.Log.e("AppLimitManager", "Error getting usage", e)
            e.printStackTrace()
            return 0L
        }
    }
    /**
     * Get total app usage for today in minutes (public method)
     * Used to determine minimum limit that can be set
     */
    fun getTotalAppUsageMinutes(packageName: String): Int {
        return getCurrentAppUsageMinutes(packageName)
    }

    /**
     * Refresh usage for all limits so UI can show up-to-date minutes.
     */
    suspend fun refreshUsageForAllLimits() {
        try {
            val limits = appLimitDao.getAllLimitsOnce()
            limits.forEach { limit ->
                val usageMillis = getCurrentAppUsageMillis(limit.packageName)
                val usageMinutes = (usageMillis / 60000L).toInt()
                val isBlocked = limit.isEnabled && usageMillis >= limit.limitMinutes * 60L * 1000L
                appLimitDao.updateUsageAndBlockStatus(limit.packageName, usageMinutes, isBlocked)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLimitManager", "Error refreshing usage for limits", e)
        }
    }

    /**
     * Send user to home screen (exits the blocked app)
     */
    fun sendToHomeScreen() {
        try {
            android.util.Log.d("AppLimitManager", "Sending user to home screen")
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(homeIntent)
            android.util.Log.d("AppLimitManager", "Home intent started successfully")
        } catch (e: Exception) {
            android.util.Log.e("AppLimitManager", "Error sending to home", e)
            e.printStackTrace()
        }
    }
    /**
     * Get foreground app package name
     */
    fun getForegroundApp(): String? {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000 * 60,
                now
            )
            if (usageStats.isEmpty()) return null
            val sortedStats = usageStats.sortedByDescending { it.lastTimeUsed }
            return sortedStats.firstOrNull()?.packageName
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    /**
     * Reset all daily usage
     */
    suspend fun resetDailyUsage() {
        appLimitDao.resetDailyUsage(LocalDate.now().toString())
    }

    /**
     * Save app usage session for history tracking
     * Saves the incremental usage since last check
     */
    suspend fun saveUsageSession(packageName: String, totalUsedMinutesNow: Int) {
        try {
            if (database == null) {
                android.util.Log.w("AppLimitManager", "Database not available for saving session")
                return
            }

            val now = System.currentTimeMillis()
            val today = LocalDate.now().toString()

            // Calculate session duration as 1 minute minimum (since we check every 5 seconds)
            // This represents the incremental change
            val sessionDuration = maxOf(1, totalUsedMinutesNow % 60) // Get minutes part

            val session = AppUsageSession(
                packageName = packageName,
                date = today,
                startTime = now - 60000,  // Approximate: 1 minute ago
                endTime = now,
                durationMinutes = 1  // Always record 1-minute increments to avoid duplicates
            )

            database.appUsageSessionDao().insertSession(session)
            android.util.Log.d("AppLimitManager", "✅ Saved 1-min session: $packageName (total: $totalUsedMinutesNow min) on $today")
        } catch (e: Exception) {
            android.util.Log.e("AppLimitManager", "Error saving usage session", e)
            e.printStackTrace()
        }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val appIcon: android.graphics.drawable.Drawable
)
