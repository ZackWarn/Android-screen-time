package com.example.screentime.data.repository

import com.example.screentime.data.dao.WeeklyStatsDao
import com.example.screentime.data.entities.WeeklyStats

class WeeklyStatsRepository(private val weeklyStatsDao: WeeklyStatsDao) {

    suspend fun getStatsForWeek(weekNumber: Int, year: Int): WeeklyStats? {
        return weeklyStatsDao.getStatsByWeek(weekNumber, year)
    }

    suspend fun getOrCreateStatsForWeek(weekNumber: Int, year: Int): WeeklyStats {
        return weeklyStatsDao.getStatsByWeek(weekNumber, year)
            ?: WeeklyStats(weekNumber = weekNumber, year = year)
    }

    suspend fun updateStats(stats: WeeklyStats) {
        weeklyStatsDao.updateStats(stats)
    }

    suspend fun insertStats(stats: WeeklyStats) {
        weeklyStatsDao.insertStats(stats)
    }

    suspend fun getLastCompletedWeek(): WeeklyStats? {
        return weeklyStatsDao.getLastCompletedWeek()
    }
}

