package com.areka.app.feature.practice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.repository.IMistakeRepository
import com.areka.app.core.repository.IProgressRepository
import com.areka.app.core.repository.IQuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PracticeViewModel(
    private val questionBank: QuestionBank,
    private val progressRepository: IProgressRepository,
    private val quizRepository: IQuizRepository,
    private val mistakeRepository: IMistakeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        loadPracticeData()
        observeRecentAttempts()
    }

    private fun observeRecentAttempts() {
        viewModelScope.launch {
            quizRepository.observeAllAttempts().collect { attempts ->
                _uiState.update { it.copy(recentAttempts = attempts) }
            }
        }
        viewModelScope.launch {
            mistakeRepository.openMistakes.collect { mistakes ->
                _uiState.update { it.copy(openMistakesCount = mistakes.size) }
            }
        }
    }

    fun loadPracticeData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val subjects = questionBank.getSubjects()
                val initialSubjectId = _uiState.value.selectedSubjectId ?: subjects.firstOrNull()?.id ?: "math"
                val units = questionBank.getUnits(initialSubjectId)
                val recs = progressRepository.getRecommendations()
                val weak = progressRepository.getWeakAreas(10)
                val openMistakes = mistakeRepository.openMistakes.value.size

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subjects = subjects,
                        selectedSubjectId = initialSubjectId,
                        subjectUnits = units,
                        recommendations = recs,
                        weakAreas = weak,
                        openMistakesCount = openMistakes,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load practice") }
            }
        }
    }

    fun selectCategory(category: PracticeCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectSubjectFilter(subjectId: String) {
        val units = questionBank.getUnits(subjectId)
        _uiState.update {
            it.copy(
                selectedSubjectId = subjectId,
                subjectUnits = units
            )
        }
    }

    companion object {
        fun provideFactory(
            questionBank: QuestionBank,
            progressRepository: IProgressRepository,
            quizRepository: IQuizRepository,
            mistakeRepository: IMistakeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PracticeViewModel(
                    questionBank,
                    progressRepository,
                    quizRepository,
                    mistakeRepository
                ) as T
            }
        }
    }
}
