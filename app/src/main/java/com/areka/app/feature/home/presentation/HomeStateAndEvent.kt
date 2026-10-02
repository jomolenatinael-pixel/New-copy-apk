package com.areka.app.feature.home.presentation

import com.areka.app.core.repository.StudyRecommendation
import com.areka.app.data.model.SubjectProgress
import com.areka.app.data.model.UserProfile

data class ContinueStudyItem(
    val title: String,
    val subtitle: String,
    val detail: String,
    val subjectId: String,
    val unitId: String,
    val isQuiz: Boolean = true,
    val isFlashcard: Boolean = false,
    val isMistake: Boolean = false
)

data class TodayPlanItem(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val subjectId: String,
    val unitId: String,
    val actionType: PlanActionType
)

enum class PlanActionType {
    REVIEW_MISTAKES,
    STUDY_FLASHCARDS,
    PRACTICE_QUIZ
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val greeting: String = "Good morning",
    val subjects: List<SubjectProgress> = emptyList(),
    val continueItem: ContinueStudyItem? = null,
    val todayPlan: List<TodayPlanItem> = emptyList(),
    val dueFlashcards: Int = 0,
    val openMistakes: Int = 0,
    val error: String? = null
)

sealed interface HomeEvent {
    data class OpenSubject(val subjectId: String) : HomeEvent
    data class OpenUnitQuiz(val unitId: String, val subjectId: String) : HomeEvent
    data class OpenFlashcards(val unitId: String, val subjectId: String) : HomeEvent
    data class OpenMistakes(val unitId: String? = null) : HomeEvent
    data object QuickPractice : HomeEvent
    data object QuickFlashcards : HomeEvent
    data object QuickProgress : HomeEvent
    data object Refresh : HomeEvent
}
