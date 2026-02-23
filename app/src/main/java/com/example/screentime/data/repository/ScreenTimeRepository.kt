package com.example.screentime.data.repository

import com.example.screentime.data.dao.DailyProgressDao
import com.example.screentime.data.entities.DailyProgress
import java.time.LocalDate

class ScreenTimeRepository(private val dailyProgressDao: DailyProgressDao) {

    suspend fun addDailyProgress(progress: DailyProgress) {
        dailyProgressDao.insertProgress(progress)
    }

    suspend fun getDailyProgress(date: LocalDate): DailyProgress? {
        return dailyProgressDao.getProgressByDate(date)
    }

    suspend fun getWeeklyProgress(startDate: LocalDate, endDate: LocalDate): List<DailyProgress> {
        return dailyProgressDao.getProgressRange(startDate, endDate)
    }

    suspend fun updateDailyProgress(progress: DailyProgress) {
        dailyProgressDao.updateProgress(progress)
    }

    suspend fun getTotalScreenTime(startDate: LocalDate, endDate: LocalDate): Int {
        return dailyProgressDao.getTotalScreenTimeBetween(startDate, endDate) ?: 0
    }

    suspend fun getAverageScreenTime(startDate: LocalDate, endDate: LocalDate): Float {
        return dailyProgressDao.getAverageScreenTimeBetween(startDate, endDate) ?: 0f
    }
}

