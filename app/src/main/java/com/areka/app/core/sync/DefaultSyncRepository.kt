package com.areka.app.core.sync

import com.areka.app.data.model.LeaderboardEntry
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScore
import com.areka.app.data.model.UserProfile
import com.areka.app.data.remote.AuthState
import com.areka.app.data.remote.SupabaseAuth
import com.areka.app.data.remote.SupabaseCloudSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class DefaultSyncRepository : SyncRepository {
    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow<String?>(null)
    override val lastSyncMessage: StateFlow<String?> = _lastSyncMessage.asStateFlow()

    override val leaderboard: StateFlow<List<LeaderboardEntry>> = SupabaseCloudSync.leaderboard

    override suspend fun syncAll(profile: UserProfile): Boolean {
        val auth = SupabaseAuth.state.value
        if (auth !is AuthState.SignedIn) {
            _syncStatus.value = SyncStatus.OFFLINE
            _lastSyncMessage.value = "Guest mode: local offline storage"
            return false
        }

        _syncStatus.value = SyncStatus.SYNCING
        _lastSyncMessage.value = "Syncing with cloud..."

        return try {
            SupabaseCloudSync.drainPendingAttempts()
            val profileRes = SupabaseCloudSync.syncProfile(profile)
            val adminRes = SupabaseCloudSync.syncAdminFlag()
            val lbRes = SupabaseCloudSync.refreshLeaderboard()

            if (profileRes.isSuccess && (lbRes.isSuccess || lbRes.exceptionOrNull() is IOException)) {
                _syncStatus.value = SyncStatus.SYNCED
                _lastSyncMessage.value = "All changes synchronized"
                true
            } else {
                _syncStatus.value = SyncStatus.SYNC_FAILED
                _lastSyncMessage.value = profileRes.exceptionOrNull()?.message ?: "Sync encountered an issue"
                false
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.SYNC_FAILED
            _lastSyncMessage.value = e.message ?: "Cloud sync failed"
            false
        }
    }

    override suspend fun recordQuizAttempt(
        quiz: Quiz,
        score: QuizScore,
        completedAtIso: String,
        localAttemptId: String?
    ): Result<Unit> {
        val auth = SupabaseAuth.state.value
        if (auth !is AuthState.SignedIn) {
            return Result.success(Unit) // Offline / guest is stored locally in Room
        }
        return SupabaseCloudSync.pushQuizAttempt(quiz, score, completedAtIso, localAttemptId)
    }

    override suspend fun syncProfile(profile: UserProfile): Result<Unit> {
        val auth = SupabaseAuth.state.value
        if (auth !is AuthState.SignedIn) return Result.success(Unit)
        return SupabaseCloudSync.syncProfile(profile)
    }

    override suspend fun refreshLeaderboard(): Result<List<LeaderboardEntry>> {
        val auth = SupabaseAuth.state.value
        if (auth !is AuthState.SignedIn) return Result.success(emptyList())
        return SupabaseCloudSync.refreshLeaderboard()
    }

    override suspend fun syncAdminFlag(): Result<Boolean> {
        val auth = SupabaseAuth.state.value
        if (auth !is AuthState.SignedIn) return Result.success(false)
        return SupabaseCloudSync.syncAdminFlag()
    }
}
