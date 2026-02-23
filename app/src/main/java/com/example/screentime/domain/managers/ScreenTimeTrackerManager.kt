package com.example.screentime.domain.managers

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import java.util.Calendar

class ScreenTimeTrackerManager(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * Get total screen time for a given day in minutes
     * Returns 0 if permission is not granted
     */
    fun getDailyScreenTime(dayInMillis: Long = System.currentTimeMillis()): Int {
        return try {
            if (!hasUsageStatsPermission()) {
                return 0
            }

            val calendar = Calendar.getInstance().apply {
                timeInMillis = dayInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            val startTime = calendar.timeInMillis
            val endTime = startTime + 24 * 60 * 60 * 1000 // One day in milliseconds

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            var totalScreenTime = 0L

            for (stats in usageStats) {
                // Skip system apps and our own app
                if (isSystemApp(stats.packageName) || stats.packageName == context.packageName) {
                    continue
                }
                totalScreenTime += stats.totalTimeInForeground
            }

            (totalScreenTime / 1000 / 60).toInt() // Convert to minutes
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Check if PACKAGE_USAGE_STATS permission is granted
     */
    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun isSystemApp(packageName: String): Boolean {
        return packageName.startsWith("android.") ||
                packageName.startsWith("com.android.") ||
                packageName == "com.google.android.gms"
    }
}

