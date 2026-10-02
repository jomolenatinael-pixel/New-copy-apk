package com.areka.app.feature.flashcards.presentation

import com.areka.app.data.local.ReviewGrade
import com.areka.app.data.model.Flashcard

data class FlashcardUiState(
    val isLoading: Boolean = false,
    val subjectId: String = "",
    val unitId: String = "",
    val subjectName: String = "",
    val unitTitle: String = "",
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val reviewedCount: Int = 0,
    val masteredCount: Int = 0,
    val isSessionFinished: Boolean = false,
    val againInterval: String = "<1m",
    val hardInterval: String = "1.5m",
    val goodInterval: String = "10m",
    val easyInterval: String = "4d",
    val error: String? = null
)

sealed interface FlashcardEvent {
    data object FlipCard : FlashcardEvent
    data class Grade(val grade: ReviewGrade) : FlashcardEvent
    data object RestartSession : FlashcardEvent
    data object ExitSession : FlashcardEvent
}
