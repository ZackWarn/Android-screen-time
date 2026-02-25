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

        // Initialize session saved times to now so we don't save duplicate sessions on restart
        sessionSavedApps.clear()

        // Register broadcast receiver for limit changes
        limitChangeReceiver = LimitChangeReceiver()
        val filter = IntentFilter("com.example.screentime.LIMIT_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(limitChangeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(limitChangeReceiver, filter)
        }

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
        overlayBlockManager.removeOverlay()

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
                android.util.Log.d("AppMonitorService", "📱 [CHECK] Checking app: $currentApp")

                if (currentApp != null && currentApp != packageName) {
                    android.util.Log.d("AppMonitorService", "📱 [CHECK] Current app: $currentApp, Last checked: $lastCheckedApp")

                    // Always check the app usage
                    val status = appLimitManager.checkAppUsage(currentApp)
                    android.util.Log.d("AppMonitorService", "📊 [STATUS] Status for $currentApp: ${status::class.simpleName}")

                    // Save usage session periodically (once per minute per app) to avoid duplicate saves
                    if (status is LimitStatus.WithinLimit || status is LimitStatus.Exceeded) {
                        val usedMinutes = when (status) {
                            is LimitStatus.WithinLimit -> status.usedMinutes
                            is LimitStatus.Exceeded -> status.usedMinutes
                            else -> 0
                        }

                        if (usedMinutes > 0) {
                            val now = System.currentTimeMillis()
                            val lastSavedTime = sessionSavedApps[currentApp] ?: 0L
                            val timeSinceLastSave = now - lastSavedTime

                            // Save session only if 60+ seconds have passed since last save
                            if (timeSinceLastSave >= 60_000L) {
                                android.util.Log.d("AppMonitorService", "💾 [SAVE] Saving session: $currentApp = $usedMinutes min (time since last save: ${timeSinceLastSave/1000}s)")
                                appLimitManager.saveUsageSession(currentApp, usedMinutes)
                                sessionSavedApps[currentApp] = now
                            } else {
                                android.util.Log.d("AppMonitorService", "⏳ [SKIP] Not saving yet: ${(60_000L - timeSinceLastSave)/1000}s until next save for $currentApp")
                            }
                        }
                    }

                    when (status) {
                        is LimitStatus.Exceeded -> {
                            android.util.Log.d("AppMonitorService", "🚨 [EXCEEDED] LIMIT EXCEEDED for $currentApp: ${status.usedMinutes}/${status.limitMinutes} min")

                            // Get app name
                            val appName = try {
                                val pm = applicationContext.packageManager
                                val appInfo = pm.getApplicationInfo(currentApp, 0)
                                pm.getApplicationLabel(appInfo).toString()
                            } catch (e: Exception) {
                                currentApp
                            }

                            // Check overlay permission status
                            val hasOverlay = overlayBlockManager.hasOverlayPermission()
                            android.util.Log.d("AppMonitorService", "🔍 [CHECK] Overlay permission: $hasOverlay")

                            // Always show overlay when app is exceeded
                            // This ensures overlay shows even when app is closed and reopened
                            if (hasOverlay) {
                                android.util.Log.d("AppMonitorService", "🚫 [OVERLAY] Attempting to show overlay for $appName")
                                handler.post {
                                    android.util.Log.d("AppMonitorService", "🚫 [OVERLAY] Handler.post executing for $appName")
                                    overlayBlockManager.showBlockingOverlay(appName, status.usedMinutes, status.limitMinutes)
                                }
                            } else {
                                android.util.Log.d("AppMonitorService", "⚠️ [NO-OVERLAY] No overlay permission, using notification")
                            }

                            // ALWAYS show notification regardless of overlay
                            android.util.Log.d("AppMonitorService", "📢 [NOTIFY] Showing limit exceeded notification for $currentApp")
                            showLimitExceededNotification(currentApp, status.usedMinutes, status.limitMinutes)
                        }
                        is LimitStatus.WithinLimit -> {
                            android.util.Log.d("AppMonitorService", "✅ [WITHIN] Within limit for $currentApp: ${status.usedMinutes}/${status.limitMinutes} min")
                            val remaining = status.limitMinutes - status.usedMinutes
                            if (remaining <= 5 && remaining > 0) {
                                // Warn user when 5 minutes or less remaining
                                android.util.Log.d("AppMonitorService", "⚠️  [WARNING] Only $remaining min remaining for $currentApp")
                                showWarningNotification(currentApp, remaining)
                            }
                        }
                        else -> {
                            android.util.Log.d("AppMonitorService", "ℹ️  [NO LIMIT] No limit set for $currentApp")
                        }
                    }

                    // Update last checked app AFTER processing status
                    lastCheckedApp = currentApp
                }
            } catch (e: Exception) {
                android.util.Log.e("AppMonitorService", "❌ [ERROR] Error checking app", e)
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
    private fun showLimitExceededNotification(packageName: String, usedMinutes: Int, limitMinutes: Int) {
        try {
            // Create intent to go to home screen immediately
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

            // Build heads-up notification that appears from top
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⏱️ Time's Up!")
                .setContentText("You've used $usedMinutes of $limitMinutes minutes.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("You've reached your daily limit for this app.\n\nUsed: $usedMinutes minutes\nLimit: $limitMinutes minutes\n\nTake a break and try again tomorrow!"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(blockedPendingIntent, true)
                .setContentIntent(homePendingIntent)  // Tap notification goes home
                .setAutoCancel(true)
                .setOngoing(false)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTimeoutAfter(10000)  // Auto-dismiss after 10 seconds
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

            android.util.Log.d("AppMonitorService", "✅ Heads-up notification sent for $packageName (ID: $notificationId)")

            // Immediately send user to home screen
            try {
                homePendingIntent.send()
                android.util.Log.d("AppMonitorService", "🏠 Sent to home screen")
            } catch (e: Exception) {
                android.util.Log.e("AppMonitorService", "Failed to send home intent", e)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppMonitorService", "Error showing limit exceeded notification", e)
            e.printStackTrace()
        }
    }


    private fun showWarningNotification(packageName: String, remainingMinutes: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("App Limit Warning")
            .setContentText("Only $remainingMinutes minutes remaining for this app")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(packageName.hashCode() + 1000, notification)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
}
