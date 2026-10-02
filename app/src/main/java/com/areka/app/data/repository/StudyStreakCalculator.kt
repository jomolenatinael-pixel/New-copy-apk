package com.areka.app.data.repository

object StudyStreakCalculator {
    fun nextStreak(currentStreak: Int, lastActiveEpochDay: Long, todayEpochDay: Long): Int {
        if (lastActiveEpochDay <= 0L) {
            return 1
        }
        val diff = todayEpochDay - lastActiveEpochDay
        return when {
            diff <= 0L -> currentStreak.coerceAtLeast(1)
            diff == 1L -> currentStreak + 1
            else -> 1
        }
    }
}
