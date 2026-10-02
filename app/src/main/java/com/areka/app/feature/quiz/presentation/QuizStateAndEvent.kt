package com.areka.app.feature.quiz.presentation

import com.areka.app.data.local.MistakeEntity
import com.areka.app.data.model.Question
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScore

data class QuizUiState(
    val quiz: Quiz? = null,
    val currentQuestionIndex: Int = 0,
    val userAnswers: Map<Int, String> = emptyMap(),
    val isSubmittedForCurrent: Boolean = false,
    val selectedOptionForCurrent: String? = null,
    val fillBlankInput: String = "",
    val timeSpentSeconds: Int = 0,
    val isCompleted: Boolean = false,
    val score: QuizScore? = null,
    val mistakes: List<MistakeEntity> = emptyList(),
    val masteredQuestions: List<Question> = emptyList(),
    val reviewNeededQuestions: List<Question> = emptyList()
)

sealed interface QuizEvent {
    data class SelectOption(val optionId: String) : QuizEvent
    data class UpdateFillBlankInput(val input: String) : QuizEvent
    data object SubmitAnswer : QuizEvent
    data object NextQuestion : QuizEvent
    data object RetakeQuiz : QuizEvent
    data object ExitQuiz : QuizEvent
    data object ReviewMistakes : QuizEvent
}
