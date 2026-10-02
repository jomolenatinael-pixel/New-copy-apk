package com.areka.app.core.sync

import com.areka.app.data.model.LeaderboardEntry
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScore
import com.areka.app.data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    SYNC_FAILED
}

interface SyncRepository {
    val syncStatus: StateFlow<SyncStatus>
    val lastSyncMessage: StateFlow<String?>
    val leaderboard: StateFlow<List<LeaderboardEntry>>

    suspend fun syncAll(profile: UserProfile): Boolean
    suspend fun recordQuizAttempt(
        quiz: Quiz,
        score: QuizScore,
        completedAtIso: String,
        localAttemptId: String? = null
    ): Result<Unit>
    suspend fun syncProfile(profile: UserProfile): Result<Unit>
    suspend fun refreshLeaderboard(): Result<List<LeaderboardEntry>>
    suspend fun syncAdminFlag(): Result<Boolean>
}
