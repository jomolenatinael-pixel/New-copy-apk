package com.areka.app.navigation

import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScore

enum class MainTab {
    HOME,
    LEARN,
    PRACTICE,
    PROGRESS,
    YOU
}

sealed interface NavRoute {
    // Primary Tab Roots
    data object HomeRoot : NavRoute
    data object LearnRoot : NavRoute
    data object PracticeRoot : NavRoute
    data object ProgressRoot : NavRoute
    data object YouRoot : NavRoute

    // Hierarchical Sub-screens
    data class SubjectDetail(val subjectId: String) : NavRoute
    data class UnitDetail(val unitId: String, val subjectId: String) : NavRoute
    data class ActiveQuiz(val quizId: String, val unitId: String? = null, val subjectId: String? = null) : NavRoute
    data class QuizResult(
        val quiz: Quiz,
        val score: QuizScore,
        val timeSpentSeconds: Int
    ) : NavRoute
    data class FlashcardStudy(val unitId: String, val subjectId: String) : NavRoute
    data class MistakesReview(val unitId: String? = null) : NavRoute
    data class Auth(val isSignUp: Boolean = false) : NavRoute
}
