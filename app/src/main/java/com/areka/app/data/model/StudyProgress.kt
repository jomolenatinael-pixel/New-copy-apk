package com.areka.app.data.model

data class UnitProgress(
    val unitId: String,
    val quizAttempts: Int,
    val quizAccuracyPercent: Int,
    val flashcardsReviewed: Int,
    val masteryPercent: Int,
    val lastStudiedAtEpochMillis: Long?
)

data class SubjectProgress(
    val subjectId: String,
    val subjectName: String,
    val iconType: String,
    val accentColorHex: Long,
    val completedUnits: Int,
    val totalUnits: Int,
    val averageScorePercent: Int,
    val masteryPercent: Int,
    val lastStudiedAtEpochMillis: Long?
)

