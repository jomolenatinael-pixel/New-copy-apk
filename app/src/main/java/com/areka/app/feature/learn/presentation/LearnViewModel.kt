package com.areka.app.feature.learn.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.repository.IProgressRepository
import com.areka.app.core.repository.IQuizRepository
import com.areka.app.data.model.SubjectProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LearnViewModel(
    private val questionBank: QuestionBank,
    private val progressRepository: IProgressRepository,
    private val quizRepository: IQuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    private var rawSubjectProgress = listOf<SubjectProgress>()

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val subjects = questionBank.getSubjects()
                rawSubjectProgress = progressRepository.getAllSubjectProgress()
                filterSubjects("")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allSubjects = subjects
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load subjects") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterSubjects(query)
    }

    private fun filterSubjects(query: String) {
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            rawSubjectProgress
        } else {
            rawSubjectProgress.filter {
                it.subjectName.contains(trimmed, ignoreCase = true) ||
                        it.subjectId.contains(trimmed, ignoreCase = true)
            }
        }
        _uiState.update { it.copy(filteredSubjects = filtered) }
    }

    fun selectSubject(subjectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val subject = questionBank.getSubject(subjectId)
            val progress = progressRepository.getSubjectProgress(subjectId)
            val units = questionBank.getUnits(subjectId)

            val unitsWithProg = units.map { unit ->
                val uProg = quizRepository.getUnitProgress(unit.id)
                UnitWithProgress(unit, uProg)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedSubject = subject,
                    selectedSubjectProgress = progress,
                    selectedSubjectUnits = unitsWithProg,
                    selectedUnit = null
                )
            }
        }
    }

    fun selectUnit(unitId: String, subjectId: String) {
        viewModelScope.launch {
            val unit = questionBank.getUnit(unitId)
            val uProg = quizRepository.getUnitProgress(unitId)
            val flashcards = questionBank.getFlashcards(unitId)

            _uiState.update {
                it.copy(
                    selectedUnit = unit,
                    selectedUnitProgress = uProg,
                    selectedUnitFlashcards = flashcards
                )
            }
        }
    }

    fun clearSelectedUnit() {
        _uiState.update { it.copy(selectedUnit = null, selectedUnitProgress = null, selectedUnitFlashcards = emptyList()) }
    }

    fun clearSelectedSubject() {
        _uiState.update {
            it.copy(
                selectedSubject = null,
                selectedSubjectProgress = null,
                selectedSubjectUnits = emptyList(),
                selectedUnit = null
            )
        }
        loadSubjects()
    }

    companion object {
        fun provideFactory(
            questionBank: QuestionBank,
            progressRepository: IProgressRepository,
            quizRepository: IQuizRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LearnViewModel(questionBank, progressRepository, quizRepository) as T
            }
        }
    }
}
