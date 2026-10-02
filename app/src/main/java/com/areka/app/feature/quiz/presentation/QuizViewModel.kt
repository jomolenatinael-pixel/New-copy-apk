package com.areka.app.feature.quiz.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.repository.IQuizRepository
import com.areka.app.data.local.MistakeEntity
import com.areka.app.data.local.currentOwnerId
import com.areka.app.data.model.Question
import com.areka.app.data.model.QuestionType
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScoring
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class QuizViewModel(
    private val quizRepository: IQuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun loadQuiz(quiz: Quiz) {
        timerJob?.cancel()
        _uiState.value = QuizUiState(
            quiz = quiz,
            currentQuestionIndex = 0,
            userAnswers = emptyMap(),
            isSubmittedForCurrent = false,
            selectedOptionForCurrent = null,
            fillBlankInput = "",
            timeSpentSeconds = 0,
            isCompleted = false
        )
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _uiState.update { it.copy(timeSpentSeconds = it.timeSpentSeconds + 1) }
            }
        }
    }

    fun selectOption(optionId: String) {
        if (_uiState.value.isSubmittedForCurrent) return
        _uiState.update { it.copy(selectedOptionForCurrent = optionId) }
    }

    fun updateFillBlank(input: String) {
        if (_uiState.value.isSubmittedForCurrent) return
        _uiState.update { it.copy(fillBlankInput = input) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        val currentQuestion = quiz.questions.getOrNull(state.currentQuestionIndex) ?: return

        val answer = if (currentQuestion.type == QuestionType.FILL_IN_THE_BLANK) {
            state.fillBlankInput.trim()
        } else {
            state.selectedOptionForCurrent ?: return
        }

        val updatedAnswers = state.userAnswers + (currentQuestion.id to answer)
        _uiState.update {
            it.copy(
                userAnswers = updatedAnswers,
                isSubmittedForCurrent = true
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val quiz = state.quiz ?: return

        if (state.currentQuestionIndex < quiz.questions.lastIndex) {
            val nextIndex = state.currentQuestionIndex + 1
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    isSubmittedForCurrent = false,
                    selectedOptionForCurrent = null,
                    fillBlankInput = ""
                )
            }
        } else {
            finishQuiz()
        }
    }

    private fun finishQuiz() {
        timerJob?.cancel()
        val state = _uiState.value
        val quiz = state.quiz ?: return
        val answers = state.userAnswers
        val score = QuizScoring.calculate(quiz, answers)

        val mastered = mutableListOf<Question>()
        val reviewNeeded = mutableListOf<Question>()
        val mistakes = mutableListOf<MistakeEntity>()
        val owner = currentOwnerId()

        quiz.questions.forEach { q ->
            val submitted = answers[q.id]
            val isCorrect = QuizScoring.isCorrect(q, submitted)
            if (isCorrect) {
                mastered.add(q)
            } else {
                reviewNeeded.add(q)
                val submittedText = if (q.type == QuestionType.FILL_IN_THE_BLANK) {
                    submitted ?: "Skipped"
                } else {
                    q.options.find { it.id == submitted }?.text ?: submitted ?: "Skipped"
                }
                val correctText = if (q.type == QuestionType.FILL_IN_THE_BLANK) {
                    q.correctOptionId
                } else {
                    q.options.find { it.id == q.correctOptionId }?.text ?: q.correctOptionId
                }

                mistakes.add(
                    MistakeEntity(
                        ownerUserId = owner,
                        quizId = quiz.id,
                        questionId = q.id,
                        questionText = q.text,
                        selectedAnswer = submittedText,
                        correctAnswer = correctText,
                        subjectId = quiz.subjectId ?: "",
                        unitId = quiz.unitId ?: "",
                        explanation = q.explanation,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        viewModelScope.launch {
            quizRepository.recordQuizResult(
                quiz = quiz,
                score = score,
                timeSpentSeconds = state.timeSpentSeconds,
                mistakes = mistakes
            )
        }

        _uiState.update {
            it.copy(
                isCompleted = true,
                score = score,
                mistakes = mistakes,
                masteredQuestions = mastered,
                reviewNeededQuestions = reviewNeeded
            )
        }
    }

    fun retakeQuiz() {
        val quiz = _uiState.value.quiz ?: return
        loadQuiz(quiz)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun provideFactory(quizRepository: IQuizRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QuizViewModel(quizRepository) as T
                }
            }
    }
}
