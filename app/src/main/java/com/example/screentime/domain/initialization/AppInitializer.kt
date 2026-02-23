package com.example.screentime.domain.initialization

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.screentime.domain.workers.DailyScreenTimeWorker
import com.example.screentime.domain.workers.WeeklyResetWorker
import java.util.concurrent.TimeUnit

object AppInitializer {

    fun initializeApp(context: Context) {
        // Schedule background workers
        scheduleDailyScreenTimeTask(context)
        scheduleWeeklyResetTask(context)
    }

    private fun scheduleDailyScreenTimeTask(context: Context) {
        try {
            val dailyScreenTimeRequest = PeriodicWorkRequestBuilder<DailyScreenTimeWorker>(
                1, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_screen_time",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyScreenTimeRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleWeeklyResetTask(context: Context) {
        try {
            val weeklyResetRequest = PeriodicWorkRequestBuilder<WeeklyResetWorker>(
                7, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "weekly_reset",
                ExistingPeriodicWorkPolicy.KEEP,
                weeklyResetRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

