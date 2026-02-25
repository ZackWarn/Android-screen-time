package com.example.screentime.domain.models

/**
 * Represents the status of an app's usage relative to its limit
 */
sealed class LimitStatus {
    /**
     * App has exceeded its daily time limit
     */
    data class Exceeded(
        val usedMinutes: Int,
        val limitMinutes: Int
    ) : LimitStatus()

    /**
     * App is within its daily time limit
     */
    data class WithinLimit(
        val usedMinutes: Int,
        val limitMinutes: Int
    ) : LimitStatus()

    /**
     * No limit is set for this app
     */
    object NoLimit : LimitStatus()
}

