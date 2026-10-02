package com.areka.app.feature.flashcards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.repository.IFlashcardRepository
import com.areka.app.data.local.CardStatus
import com.areka.app.data.local.FlashcardScheduleEntity
import com.areka.app.data.local.ReviewGrade
import com.areka.app.data.local.currentOwnerId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardViewModel(
    private val flashcardRepository: IFlashcardRepository,
    private val questionBank: QuestionBank
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private var currentSchedulesMap = mutableMapOf<String, FlashcardScheduleEntity>()

    fun loadDeck(unitId: String, subjectId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val subject = questionBank.getSubject(subjectId)
                val unit = questionBank.getUnit(unitId)
                val cards = questionBank.getFlashcards(unitId)
                val schedules = flashcardRepository.getSchedulesForUnit(unitId)
                currentSchedulesMap = schedules.associateBy { it.cardId }.toMutableMap()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subjectId = subjectId,
                        unitId = unitId,
                        subjectName = subject?.name ?: "",
                        unitTitle = unit?.title ?: "",
                        cards = cards,
                        currentIndex = 0,
                        isFlipped = false,
                        reviewedCount = 0,
                        masteredCount = 0,
                        isSessionFinished = false
                    )
                }
                updateIntervalPreviews()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load deck") }
            }
        }
    }

    fun flipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun gradeCard(grade: ReviewGrade) {
        val state = _uiState.value
        val card = state.cards.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            val updatedSchedule = flashcardRepository.gradeCard(
                cardId = card.id,
                subjectId = state.subjectId,
                unitId = state.unitId,
                grade = grade
            )
            currentSchedulesMap[card.id] = updatedSchedule

            val isMastered = updatedSchedule.status == CardStatus.REVIEW.name
            val newMastered = if (isMastered) state.masteredCount + 1 else state.masteredCount
            val newReviewed = state.reviewedCount + 1

            if (state.currentIndex < state.cards.lastIndex) {
                _uiState.update {
                    it.copy(
                        currentIndex = it.currentIndex + 1,
                        isFlipped = false,
                        reviewedCount = newReviewed,
                        masteredCount = newMastered
                    )
                }
                updateIntervalPreviews()
            } else {
                _uiState.update {
                    it.copy(
                        isSessionFinished = true,
                        reviewedCount = newReviewed,
                        masteredCount = newMastered
                    )
                }
            }
        }
    }

    private suspend fun updateIntervalPreviews() {
        val state = _uiState.value
        val card = state.cards.getOrNull(state.currentIndex) ?: return
        val schedule = currentSchedulesMap[card.id] ?: FlashcardScheduleEntity(
            ownerUserId = currentOwnerId(),
            cardId = card.id,
            subjectId = state.subjectId,
            unitId = state.unitId,
            status = CardStatus.NEW.name
        )

        val again = flashcardRepository.getNextIntervalPreview(schedule, ReviewGrade.AGAIN)
        val hard = flashcardRepository.getNextIntervalPreview(schedule, ReviewGrade.HARD)
        val good = flashcardRepository.getNextIntervalPreview(schedule, ReviewGrade.GOOD)
        val easy = flashcardRepository.getNextIntervalPreview(schedule, ReviewGrade.EASY)

        _uiState.update {
            it.copy(
                againInterval = again,
                hardInterval = hard,
                goodInterval = good,
                easyInterval = easy
            )
        }
    }

    fun restartSession() {
        val state = _uiState.value
        loadDeck(state.unitId, state.subjectId)
    }

    companion object {
        fun provideFactory(
            flashcardRepository: IFlashcardRepository,
            questionBank: QuestionBank
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FlashcardViewModel(flashcardRepository, questionBank) as T
            }
        }
    }
}
