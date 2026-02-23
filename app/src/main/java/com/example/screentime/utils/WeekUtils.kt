package com.example.screentime.utils

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

object WeekUtils {
    private val weekFields = WeekFields.of(Locale.getDefault())

    fun getCurrentWeekNumber(): Int {
        return LocalDate.now().get(weekFields.weekOfWeekBasedYear())
    }

    fun getCurrentYear(): Int {
        return LocalDate.now().year
    }

    fun getWeekStartDate(weekNumber: Int, year: Int = getCurrentYear()): LocalDate {
        return LocalDate.of(year, 1, 1)
            .with(weekFields.weekOfWeekBasedYear(), weekNumber.toLong())
            .with(weekFields.dayOfWeek(), 1L) // Monday
    }

    fun getWeekEndDate(weekNumber: Int, year: Int = getCurrentYear()): LocalDate {
        return getWeekStartDate(weekNumber, year).plusDays(6)
    }

    fun getPreviousWeekNumber(): Int {
        val current = getCurrentWeekNumber()
        return if (current == 1) 52 else current - 1
    }

    fun getPreviousWeekYear(): Int {
        return if (getCurrentWeekNumber() == 1) getCurrentYear() - 1 else getCurrentYear()
    }
}

