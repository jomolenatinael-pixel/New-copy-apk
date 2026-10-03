package com.areka.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.repository.IFlashcardRepository
import com.areka.app.core.repository.IMistakeRepository
import com.areka.app.core.repository.IProfileRepository
import com.areka.app.core.repository.IProgressRepository
import com.areka.app.core.repository.IQuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val profileRepository: IProfileRepository,
    private val progressRepository: IProgressRepository,
    private val quizRepository: IQuizRepository,
    private val flashcardRepository: IFlashcardRepository,
    private val mistakeRepository: IMistakeRepository,
    private val questionBank: QuestionBank
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        observeLiveUpdates()
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            profileRepository.userProfile.collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
        viewModelScope.launch {
            mistakeRepository.openMistakes.collect { mistakes ->
                _uiState.update { it.copy(openMistakes = mistakes.size) }
                refreshRecommendations()
            }
        }
        viewModelScope.launch {
            flashcardRepository.observeDueCount().collect { count ->
                _uiState.update { it.copy(dueFlashcards = count) }
                refreshRecommendations()
            }
        }
    }

    fun loadHomeData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val greeting = calculateGreeting()
                val profile = profileRepository.userProfile.value
                val subjects = progressRepository.getAllSubjectProgress()
                val mistakes = mistakeRepository.openMistakes.value.size
                val dueCards = flashcardRepository.getDueCount()

                val continueItem = buildContinueItem(mistakes, dueCards)
                val todayPlan = buildTodayPlan(mistakes, dueCards)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        greeting = greeting,
                        subjects = subjects,
                        continueItem = continueItem,
                        todayPlan = todayPlan,
                        dueFlashcards = dueCards,
                        openMistakes = mistakes,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load cockpit") }
            }
        }
    }

    private fun refreshRecommendations() {
        viewModelScope.launch {
            val mistakes = mistakeRepository.openMistakes.value.size
            val dueCards = flashcardRepository.getDueCount()
            val continueItem = buildContinueItem(mistakes, dueCards)
            val todayPlan = buildTodayPlan(mistakes, dueCards)
            _uiState.update { it.copy(continueItem = continueItem, todayPlan = todayPlan) }
        }
    }

    private suspend fun buildContinueItem(openMistakes: Int, dueCards: Int): ContinueStudyItem {
        // Priority 1: Open mistakes if learner has any
        if (openMistakes > 0) {
            val firstMistake = mistakeRepository.openMistakes.value.first()
            val unit = questionBank.getUnit(firstMistake.unitId)
            return ContinueStudyItem(
                title = "Review $openMistakes Mistakes",
                subtitle = unit?.title ?: "Past Quiz Weak Points",
                detail = "Solidify questions you missed previously",
                subjectId = firstMistake.subjectId,
                unitId = firstMistake.unitId,
                isQuiz = false,
                isFlashcard = false,
                isMistake = true
            )
        }

        // Priority 2: Due flashcards
        if (dueCards > 0) {
            val allUnits = questionBank.getSubjects().flatMap { questionBank.getUnits(it.id) }
            val unitWithCards = allUnits.firstOrNull { questionBank.getFlashcards(it.id).isNotEmpty() }
            if (unitWithCards != null) {
                return ContinueStudyItem(
                    title = "Spaced Repetition Due",
                    subtitle = "${unitWithCards.title} ($dueCards cards)",
                    detail = "Review cards scheduled for today",
                    subjectId = unitWithCards.subjectId,
                    unitId = unitWithCards.id,
                    isQuiz = false,
                    isFlashcard = true,
                    isMistake = false
                )
            }
        }

        // Priority 3: Weak area or next unfinished unit from real attempt data
        val weak = progressRepository.getWeakAreas(1)
        if (weak.isNotEmpty()) {
            val w = weak.first()
            return ContinueStudyItem(
                title = "Reinforce ${w.subjectName}",
                subtitle = "Unit ${w.unitTitle}",
                detail = "Accuracy: ${w.accuracyPercent}% • Practice to achieve mastery",
                subjectId = w.subjectId,
                unitId = w.unitId,
                isQuiz = true,
                isFlashcard = false,
                isMistake = false
            )
        }

        // Default: First unit of first subject
        val firstSubject = questionBank.getSubjects().firstOrNull()
        val firstUnit = firstSubject?.let { questionBank.getUnits(it.id).firstOrNull() }
        val subjectName = firstSubject?.name ?: "Mathematics"
        val unitTitle = firstUnit?.title ?: "Relations and Functions"
        return ContinueStudyItem(
            title = "Continue $subjectName",
            subtitle = unitTitle,
            detail = "Official MoE Grade 10 Curriculum",
            subjectId = firstSubject?.id ?: "math",
            unitId = firstUnit?.id ?: "math_u1",
            isQuiz = true,
            isFlashcard = false,
            isMistake = false
        )
    }

    private suspend fun buildTodayPlan(openMistakes: Int, dueCards: Int): List<TodayPlanItem> {
        val plan = mutableListOf<TodayPlanItem>()
        var step = 1

        if (openMistakes > 0) {
            val firstMistake = mistakeRepository.openMistakes.value.first()
            plan.add(
                TodayPlanItem(
                    stepNumber = step++,
                    title = "Review mistakes",
                    subtitle = "$openMistakes questions require attention",
                    actionLabel = "Review",
                    subjectId = firstMistake.subjectId,
                    unitId = firstMistake.unitId,
                    actionType = PlanActionType.REVIEW_MISTAKES
                )
            )
        }

        // Add recommended subject quiz
        val allSubjects = questionBank.getSubjects()
        val nextSubject = allSubjects.getOrNull(1) ?: allSubjects.firstOrNull()
        if (nextSubject != null) {
            val units = questionBank.getUnits(nextSubject.id)
            val unit = units.firstOrNull()
            if (unit != null) {
                plan.add(
                    TodayPlanItem(
                        stepNumber = step++,
                        title = "Practice ${nextSubject.name}",
                        subtitle = "Unit ${unit.unitNumber}: ${unit.title}",
                        actionLabel = "Start Quiz",
                        subjectId = nextSubject.id,
                        unitId = unit.id,
                        actionType = PlanActionType.PRACTICE_QUIZ
                    )
                )
            }
        }

        if (dueCards > 0) {
            val allUnits = questionBank.getSubjects().flatMap { questionBank.getUnits(it.id) }
            val unitWithCards = allUnits.firstOrNull { questionBank.getFlashcards(it.id).isNotEmpty() }
            if (unitWithCards != null) {
                plan.add(
                    TodayPlanItem(
                        stepNumber = step++,
                        title = "Review flashcards",
                        subtitle = "$dueCards cards due for optimal retention",
                        actionLabel = "Study",
                        subjectId = unitWithCards.subjectId,
                        unitId = unitWithCards.id,
                        actionType = PlanActionType.STUDY_FLASHCARDS
                    )
                )
            }
        }

        return plan
    }

    private fun calculateGreeting(): String {
        return try {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }
        } catch (_: Exception) {
            "Good day"
        }
    }

    companion object {
        fun provideFactory(
            profileRepository: IProfileRepository,
            progressRepository: IProgressRepository,
            quizRepository: IQuizRepository,
            flashcardRepository: IFlashcardRepository,
            mistakeRepository: IMistakeRepository,
            questionBank: QuestionBank
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    profileRepository,
                    progressRepository,
                    quizRepository,
                    flashcardRepository,
                    mistakeRepository,
                    questionBank
                ) as T
            }
        }
    }
}
