package com.areka.app.feature.practice.presentation

import com.areka.app.core.repository.StudyRecommendation
import com.areka.app.core.repository.WeakArea
import com.areka.app.data.local.QuizAttemptEntity
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
    val selectedCategory: PracticeCategory = PracticeCategory.RECOMMENDED,
    val recommendations: List<StudyRecommendation> = emptyList(),
    val subjects: List<SubjectItem> = emptyList(),
    val selectedSubjectId: String? = null,
    val subjectUnits: List<SubjectUnit> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList(),
    val recentAttempts: List<QuizAttemptEntity> = emptyList(),
    val openMistakesCount: Int = 0,
    val error: String? = null
)

sealed interface PracticeEvent {
    data class SelectCategory(val category: PracticeCategory) : PracticeEvent
    data class SelectSubjectFilter(val subjectId: String) : PracticeEvent
    data class StartQuiz(val quizId: String, val unitId: String, val subjectId: String) : PracticeEvent
    data class ReviewMistakes(val unitId: String? = null) : PracticeEvent
    data class StudyFlashcards(val unitId: String, val subjectId: String) : PracticeEvent
    data object Refresh : PracticeEvent
}
