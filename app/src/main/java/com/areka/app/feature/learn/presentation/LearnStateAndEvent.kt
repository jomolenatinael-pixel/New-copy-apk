package com.areka.app.feature.learn.presentation

import com.areka.app.data.model.Flashcard
import com.areka.app.data.model.SubjectItem
import com.areka.app.data.model.SubjectProgress
import com.areka.app.data.model.SubjectUnit
import com.areka.app.data.model.UnitProgress

data class UnitWithProgress(
    val unit: SubjectUnit,
    val progress: UnitProgress
)

data class LearnUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val allSubjects: List<SubjectItem> = emptyList(),
    val filteredSubjects: List<SubjectProgress> = emptyList(),
    val selectedSubject: SubjectItem? = null,
    val selectedSubjectProgress: SubjectProgress? = null,
    val selectedSubjectUnits: List<UnitWithProgress> = emptyList(),
    val selectedUnit: SubjectUnit? = null,
    val selectedUnitProgress: UnitProgress? = null,
    val selectedUnitFlashcards: List<Flashcard> = emptyList(),
    val error: String? = null
)

sealed interface LearnEvent {
    data class SearchQueryChanged(val query: String) : LearnEvent
    data class SelectSubject(val subjectId: String) : LearnEvent
    data class SelectUnit(val unitId: String, val subjectId: String) : LearnEvent
    data class StartUnitQuiz(val unitId: String, val subjectId: String) : LearnEvent
    data class StudyUnitFlashcards(val unitId: String, val subjectId: String) : LearnEvent
    data object BackToSubjects : LearnEvent
    data object BackToUnits : LearnEvent
}
