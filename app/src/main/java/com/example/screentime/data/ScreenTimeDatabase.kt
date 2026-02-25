package com.example.screentime.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.screentime.data.dao.AppLimitDao
import com.example.screentime.data.dao.AppSettingsDao
import com.example.screentime.data.dao.AppUsageSessionDao
import com.example.screentime.data.dao.BadgeDao
import com.example.screentime.data.dao.DailyProgressDao
import com.example.screentime.data.dao.WeeklyStatsDao
import com.example.screentime.data.entities.AppLimit
import com.example.screentime.data.entities.AppSettings
import com.example.screentime.data.entities.AppUsageSession
import com.example.screentime.data.entities.BadgeDefinition
import com.example.screentime.data.entities.DailyProgress
import com.example.screentime.data.entities.EarnedBadge
import com.example.screentime.data.entities.WeeklyStats

@Database(
    entities = [
        DailyProgress::class,
        EarnedBadge::class,
        WeeklyStats::class,
        AppSettings::class,
        BadgeDefinition::class,
        AppLimit::class,
        AppUsageSession::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ScreenTimeDatabase : RoomDatabase() {
    abstract fun dailyProgressDao(): DailyProgressDao
    abstract fun badgeDao(): BadgeDao
    abstract fun weeklyStatsDao(): WeeklyStatsDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun appUsageSessionDao(): AppUsageSessionDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenTimeDatabase? = null

        fun getDatabase(context: Context): ScreenTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScreenTimeDatabase::class.java,
                    "screentime_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

