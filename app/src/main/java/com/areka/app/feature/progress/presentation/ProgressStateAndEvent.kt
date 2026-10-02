package com.areka.app.feature.progress.presentation

import com.areka.app.core.repository.OverallStudyProgress
import com.areka.app.data.model.Achievement
import com.areka.app.data.model.DailyStreakBadge
import com.areka.app.data.model.LeaderboardEntry
import com.areka.app.data.model.SubjectProgress
import com.areka.app.data.model.UserProfile

data class ProgressUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val overall: OverallStudyProgress = OverallStudyProgress(0, 0, 0, 0, 0, 0, 0),
    val subjectProgressList: List<SubjectProgress> = emptyList(),
    val streakBadges: List<DailyStreakBadge> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isLeaderboardLoading: Boolean = false,
    val error: String? = null
)

sealed interface ProgressEvent {
    data object Refresh : ProgressEvent
    data class OpenSubject(val subjectId: String) : ProgressEvent
    data object OpenMistakes : ProgressEvent
}
