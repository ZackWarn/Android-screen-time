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

        // Reset if new day (midnight reset point)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayString = today.toString()

        val lastResetDate = try {
            LocalDate.parse(appLimit.lastResetDate)
        } catch (e: Exception) {
            LocalDate.MIN
        }

        val shouldReset = lastResetDate.isBefore(today)

        if (shouldReset) {
            android.util.Log.d(
                "AppLimitManager",
                "🔄 Resetting usage at midnight for: $packageName (last reset: ${appLimit.lastResetDate}, new: $todayString)"
            )
            appLimitDao.updateUsageAndBlockStatus(packageName, 0, false)
            appLimitDao.updateLastResetDate(packageName, todayString)

            return LimitStatus.WithinLimit(0, appLimit.limitMinutes)
        }

        // Use centralized blocking state update (FIX: Issue #5 - Consistent blocking logic)
        val (currentUsageMinutes, isBlocked) = updateBlockingState(packageName, appLimit.limitMinutes, appLimit.isEnabled)

        android.util.Log.d(
            "AppLimitManager",
            "📊 Current usage for $packageName: $currentUsageMinutes min (limit: ${appLimit.limitMinutes} min)"
        )

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
            val startOfDayMillis = LocalDate.now().atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            android.util.Log.d("AppLimitManager", "Getting usage from $startOfDayMillis to $now")

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDayMillis,
                now
            )

            android.util.Log.d("AppLimitManager", "Total usage stats entries: ${usageStats.size}")

            // Calculate usage ONLY from events after startOfDayMillis (midnight)
            // Don't use totalTimeInForeground as it may include old data
            val usageEvents = usageStatsManager.queryEvents(startOfDayMillis, now)
            val event = android.app.usage.UsageEvents.Event()

            var totalUsageMillis = 0L
            var currentSessionStart = 0L
            var isAppActive = false

            // Process all events to calculate total usage since midnight
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                if (event.packageName == packageName && event.timeStamp >= startOfDayMillis) {
                    when (event.eventType) {
                        android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                            // App started - mark the session start time
                            currentSessionStart = event.timeStamp
                            isAppActive = true
                        }
                        android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                        android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> {
                            // App paused/stopped - calculate session duration
                            if (isAppActive && currentSessionStart > 0) {
                                val sessionDuration = event.timeStamp - currentSessionStart
                                totalUsageMillis += sessionDuration
                                currentSessionStart = 0L
                                isAppActive = false
                            }
                        }
                    }
                }
            }

            // If app is currently active, add the ongoing session time
            if (isAppActive && currentSessionStart > 0) {
                val ongoingSession = now - currentSessionStart
                totalUsageMillis += ongoingSession
            }

            val usageMinutes = (totalUsageMillis / 1000 / 60).toInt()

            android.util.Log.d(
                "AppLimitManager",
                "Usage for $packageName: $totalUsageMillis ms = $usageMinutes min (calculated from events since midnight)"
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
     * Centralized method to update blocking state (FIX: Issue #5 - Blocking State Inconsistency)
     * Single source of truth for blocking logic
     */
    private suspend fun updateBlockingState(packageName: String, limitMinutes: Int, isEnabled: Boolean): Pair<Int, Boolean> {
        val usageMillis = getCurrentAppUsageMillis(packageName)
        val usageMinutes = (usageMillis / 60000L).toInt()
        val isBlocked = isEnabled && usageMillis >= limitMinutes * 60L * 1000L

        appLimitDao.updateUsageAndBlockStatus(packageName, usageMinutes, isBlocked)

        return Pair(usageMinutes, isBlocked)
    }

    /**
     * Refresh usage for all limits so UI can show up-to-date minutes.
     */
    suspend fun refreshUsageForAllLimits() {
        try {
            val limits = appLimitDao.getAllLimitsOnce()
            limits.forEach { limit ->
                // Use centralized blocking state update (FIX: Issue #5)
                updateBlockingState(limit.packageName, limit.limitMinutes, limit.isEnabled)
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

            // FIX: Issue #3 - Use UsageEvents for accurate foreground detection
            // Query events from last 10 seconds instead of 1 minute of stats
            val usageEvents = usageStatsManager.queryEvents(now - 10000, now)
            val event = android.app.usage.UsageEvents.Event()
            var lastResumedApp: String? = null
            var lastResumeTime = 0L
            var lastPauseTime = 0L
            val eventMap = mutableMapOf<String, Long>()  // Track latest event per app

            // Find the most recent ACTIVITY_RESUMED event
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                when (event.eventType) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (event.timeStamp > lastResumeTime) {
                            lastResumedApp = event.packageName
                            lastResumeTime = event.timeStamp
                        }
                        eventMap[event.packageName] = event.timeStamp
                    }
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                        if (event.timeStamp > lastPauseTime) {
                            lastPauseTime = event.timeStamp
                        }
                        // Only clear if this pause is MORE recent than resume
                        if (event.packageName == lastResumedApp && event.timeStamp > lastResumeTime) {
                            lastResumedApp = null
                        }
                    }
                }
            }

            // Fallback: If UsageEvents didn't give us a result, use UsageStats
            if (lastResumedApp == null) {
                val usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - 1000 * 60 * 5,  // Last 5 minutes
                    now
                )
                if (usageStats.isNotEmpty()) {
                    lastResumedApp = usageStats.sortedByDescending { it.lastTimeUsed }.firstOrNull()?.packageName
                }
            }

            return lastResumedApp
        } catch (e: Exception) {
            android.util.Log.e("AppLimitManager", "Error getting foreground app", e)
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
