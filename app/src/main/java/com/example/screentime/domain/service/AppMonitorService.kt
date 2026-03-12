package com.example.screentime.domain.service
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.example.screentime.MainActivity
import com.example.screentime.R
import com.example.screentime.data.ScreenTimeDatabase
import com.example.screentime.domain.managers.AppLimitManager
import com.example.screentime.domain.managers.OverlayBlockManager
import com.example.screentime.domain.models.LimitStatus
import com.example.screentime.presentation.BlockedAppActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
class AppMonitorService : Service() {
    private lateinit var appLimitManager: AppLimitManager
    private lateinit var overlayBlockManager: OverlayBlockManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null
    private var lastCheckedApp: String? = null
    private val sessionSavedApps = mutableMapOf<String, Long>()  // Track last time we saved a session per app
    private val lastWarningRemaining = mutableMapOf<String, Int>() // Track last warning remaining per app
    private var lastWarningApp: String? = null
    private var lastWarningMinute: Int? = null
    private var lastAppUsageMinutes: Int = 0  // Track previous usage to detect changes
    private var overlayShownForPackage: String? = null

    private inner class LimitChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.screentime.LIMIT_CHANGED") {
                val packageName = intent.getStringExtra("packageName")
                if (packageName != null) {
                    android.util.Log.d("AppMonitorService", "🔄 Limit changed for $packageName, clearing blocked status")
                    sessionSavedApps.remove(packageName)
                    // isBlocked status in database will be updated on next check
                }
            }
        }
    }

    private var limitChangeReceiver: LimitChangeReceiver? = null
    companion object {
        const val CHANNEL_ID = "AppMonitorChannel"
        const val NOTIFICATION_ID = 1001
        const val CHECK_INTERVAL = 5000L // Check every 5 seconds
        fun startService(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        fun stopService(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
        }
    }
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("AppMonitorService", "Service onCreate called")
        val database = ScreenTimeDatabase.getDatabase(applicationContext)
        appLimitManager = AppLimitManager(applicationContext, database.appLimitDao(), database)
        overlayBlockManager = OverlayBlockManager(applicationContext)

        // Clear sessionSavedApps on service start
        sessionSavedApps.clear()

        // Register broadcast receiver for limit changes
        limitChangeReceiver = LimitChangeReceiver()
        val filter = IntentFilter("com.example.screentime.LIMIT_CHANGED")
        ContextCompat.registerReceiver(
            this,
            limitChangeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Monitoring app usage..."))
        startMonitoring()
        android.util.Log.d("AppMonitorService", "Service started and monitoring")
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        monitoringRunnable?.let { handler.removeCallbacks(it) }

        // Unregister broadcast receiver
        if (limitChangeReceiver != null) {
            unregisterReceiver(limitChangeReceiver)
        }

        serviceScope.cancel()
    }
    private fun startMonitoring() {
        android.util.Log.d("AppMonitorService", "Starting monitoring with interval: $CHECK_INTERVAL ms")
        monitoringRunnable = object : Runnable {
            override fun run() {
                checkCurrentApp()
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
        handler.post(monitoringRunnable!!)
    }
    private fun stopMonitoring() {
        monitoringRunnable?.let { handler.removeCallbacks(it) }
    }
    private fun checkCurrentApp() {
        serviceScope.launch {
            try {
                val currentApp = appLimitManager.getForegroundApp()
                android.util.Log.d("AppMonitorService", "Checking app: $currentApp")

                if (currentApp != null && currentApp != packageName) {
                    android.util.Log.d("AppMonitorService", "Current app: $currentApp, Last checked: $lastCheckedApp")

                    // Allow re-showing overlay after user leaves the previously blocked app.
                    if (overlayShownForPackage != null && overlayShownForPackage != currentApp) {
                        overlayShownForPackage = null
                    }

                    // Always check the app usage
                    val status = appLimitManager.checkAppUsage(currentApp)
                    android.util.Log.d("AppMonitorService", "Status for $currentApp: ${status::class.simpleName}")

                    // Save usage session when app changes or usage increases
                    if (status is LimitStatus.WithinLimit || status is LimitStatus.Exceeded) {
                        val usedMinutes = when (status) {
                            is LimitStatus.WithinLimit -> status.usedMinutes
                            is LimitStatus.Exceeded -> status.usedMinutes
                            else -> 0
                        }

                        // Save session if:
                        // 1. App switched (lastCheckedApp != currentApp), or
                        // 2. Usage increased by at least 1 minute
                        if (currentApp != lastCheckedApp || usedMinutes > lastAppUsageMinutes) {
                            if (usedMinutes > 0) {
                                android.util.Log.d("AppMonitorService", "💾 Saving session: $currentApp = $usedMinutes min")
                                appLimitManager.saveUsageSession(currentApp, usedMinutes)
                                lastAppUsageMinutes = usedMinutes
                            }
                        }
                    }

                    when (status) {
                        is LimitStatus.Exceeded -> {
                            android.util.Log.d("AppMonitorService", "LIMIT EXCEEDED for $currentApp: ${status.usedMinutes}/${status.limitMinutes} min")

                            // Show full-screen blocking overlay instead of heads-up notification.
                            if (overlayShownForPackage != currentApp) {
                                val appName = resolveAppName(currentApp)
                                val shown = overlayBlockManager.showBlockingOverlay(
                                    appName = appName,
                                    usedMinutes = status.usedMinutes,
                                    limitMinutes = status.limitMinutes
                                )

                                if (shown) {
                                    overlayShownForPackage = currentApp
                                    android.util.Log.d("AppMonitorService", "✅ Blocking overlay shown for $currentApp")
                                } else {
                                    android.util.Log.w("AppMonitorService", "Overlay unavailable, falling back to block activity for $currentApp")
                                    showBlockDialog(currentApp, status.usedMinutes, status.limitMinutes)
                                }
                            }
                        }
                        is LimitStatus.WithinLimit -> {
                            android.util.Log.d("AppMonitorService", "Within limit for $currentApp: ${status.usedMinutes}/${status.limitMinutes} min")
                            val remaining = status.limitMinutes - status.usedMinutes
                            // Only show warning for 5 min and 1 min remaining
                            if ((remaining == 5 || remaining == 1) && remaining > 0) {
                                android.util.Log.d("AppMonitorService", "WARNING: Only $remaining min remaining for $currentApp")
                                // Only show warning if app or minute has changed
                                if (currentApp != lastWarningApp || remaining != lastWarningMinute) {
                                    showWarningNotification(currentApp, remaining)
                                    lastWarningApp = currentApp
                                    lastWarningMinute = remaining
                                }
                              }
                        }
                        else -> {
                            if (overlayShownForPackage == currentApp) {
                                overlayShownForPackage = null
                            }
                            android.util.Log.d("AppMonitorService", "No limit set for $currentApp")
                        }
                    }

                    // Update last checked app AFTER processing status
                    lastCheckedApp = currentApp
                }
            } catch (e: Exception) {
                android.util.Log.e("AppMonitorService", "Error checking app", e)
                e.printStackTrace()
            }
        }
    }
    private fun showBlockDialog(packageName: String, usedMinutes: Int, limitMinutes: Int) {
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("packageName", packageName)
            putExtra("usedMinutes", usedMinutes)
            putExtra("limitMinutes", limitMinutes)
        }
        startActivity(intent)
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    private fun showLimitExceededNotification(packageName: String, usedMinutes: Int, limitMinutes: Int) {
        try {
            // Get app name
            val appName = try {
                val pm = applicationContext.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }

            // Create intent to go to home screen (action button only)
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val homePendingIntent = PendingIntent.getActivity(
                this,
                packageName.hashCode() + 9000,
                homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create intent for the blocked app activity
            val blockedIntent = Intent(this, BlockedAppActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                putExtra("packageName", packageName)
                putExtra("usedMinutes", usedMinutes)
                putExtra("limitMinutes", limitMinutes)
            }

            val blockedPendingIntent = PendingIntent.getActivity(
                this,
                packageName.hashCode(),
                blockedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build heads-up notification (no auto-home or full-screen intent)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⏱️ $appName - Time's Up!")
                .setContentText("You've used $usedMinutes of $limitMinutes minutes today.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$appName has reached its daily limit.\n\n✓ Used: $usedMinutes minutes\n✓ Limit: $limitMinutes minutes\n\nTake a break and return after 12 noon tomorrow!"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(blockedPendingIntent,true)
                .setContentIntent(blockedPendingIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTimeoutAfter(10000)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "Go Home",
                    homePendingIntent
                )
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Use a unique ID for each app's notification
            val notificationId = packageName.hashCode() + 5000
            notificationManager.notify(notificationId, notification)

            android.util.Log.d("AppMonitorService", "✅ Heads-up notification sent for $appName (ID: $notificationId)")
        } catch (e: Exception) {
            android.util.Log.e("AppMonitorService", "Error showing limit exceeded notification", e)
            e.printStackTrace()
        }
    }


    private fun showWarningNotification(packageName: String, remainingMinutes: Int) {
        val appName = try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$appName - Limit Warning")
            .setContentText("Only $remainingMinutes min left today.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$appName is almost at its daily limit.\n\n✓ Remaining: $remainingMinutes min\n\nLimit resets at 12 noon tomorrow!"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(8000)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(packageName.hashCode() + 1000, notification)
        android.util.Log.d("AppMonitorService", "✅ Heads-up warning notification sent for $appName (ID: ${packageName.hashCode() + 1000})")
    }
    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Monitor")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitor",
                NotificationManager.IMPORTANCE_HIGH  // HIGH for heads-up notifications
            ).apply {
                description = "Monitors app usage and enforces limits"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)  // Bypass Do Not Disturb for limit notifications
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("AppMonitorService", "✅ Notification channel created with HIGH importance")
        }
    }

    private fun getMidnightTimestamp(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
