package com.areka.app.feature.practice.presentation

import com.areka.app.core.repository.StudyRecommendation
import com.areka.app.core.repository.WeakArea
import com.areka.app.data.local.QuizAttemptEntity
import com.areka.app.data.model.LeaderboardEntry
import com.areka.app.data.model.SubjectItem
import com.areka.app.data.model.SubjectUnit

enum class PracticeCategory {
    RECOMMENDED,
    BY_SUBJECT,
    WEAK_AREAS,
    RECENT
}

data class PracticeUiState(
    val isLoading: Boolean = false,
    val subjects: List<SubjectItem> = emptyList(),
    val expandedSubjectId: String? = null,
    val subjectUnitsMap: Map<String, List<SubjectUnit>> = emptyMap(),
    val subjectCompletedUnitsMap: Map<String, Int> = emptyMap(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val currentUserId: String? = null,
    val error: String? = null,
    // Retained for backward compatibility
    val selectedCategory: PracticeCategory = PracticeCategory.BY_SUBJECT,
    val selectedSubjectId: String? = null,
    val subjectUnits: List<SubjectUnit> = emptyList(),
    val recommendations: List<StudyRecommendation> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList(),
    val recentAttempts: List<QuizAttemptEntity> = emptyList(),
    val openMistakesCount: Int = 0
)

sealed interface PracticeEvent {
    data class ToggleSubject(val subjectId: String) : PracticeEvent
    data class OpenSubject(val subjectId: String) : PracticeEvent
    data class StartQuiz(val quizId: String, val unitId: String, val subjectId: String) : PracticeEvent
    data class StudyFlashcards(val unitId: String, val subjectId: String) : PracticeEvent
    data object Refresh : PracticeEvent
    // Kept for backward compatibility
    data class SelectCategory(val category: PracticeCategory) : PracticeEvent
    data class SelectSubjectFilter(val subjectId: String) : PracticeEvent
    data class ReviewMistakes(val unitId: String? = null) : PracticeEvent
}
