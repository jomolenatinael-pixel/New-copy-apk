package com.areka.app.feature.practice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.repository.IMistakeRepository
import com.areka.app.core.repository.IProfileRepository
import com.areka.app.core.repository.IProgressRepository
import com.areka.app.core.repository.IQuizRepository
import com.areka.app.core.sync.SyncRepository
import com.areka.app.data.model.UserProfile
import com.areka.app.data.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PracticeViewModel(
    private val questionBank: QuestionBank,
    private val progressRepository: IProgressRepository,
    private val quizRepository: IQuizRepository,
    private val mistakeRepository: IMistakeRepository,
    private val syncRepository: SyncRepository? = null,
    private val profileRepository: IProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        loadPracticeData()
        observeData()
    }

    private fun observeData() {
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
        profileRepository?.let { profileRepo ->
            viewModelScope.launch {
                profileRepo.userProfile.collect { profile ->
                    refreshLeaderboard(profile)
                }
            }
        }
        syncRepository?.let { syncRepo ->
            viewModelScope.launch {
                syncRepo.leaderboard.collect { cloudLb ->
                    if (cloudLb.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                leaderboard = cloudLb.take(10),
                                isOfflineMode = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun refreshLeaderboard(profile: UserProfile) {
        val fallback = LeaderboardRepository().global(profile)
        val cloudLb = syncRepository?.leaderboard?.value.orEmpty()
        val isOffline = cloudLb.isEmpty()
        val effectiveLb = if (cloudLb.isNotEmpty()) cloudLb else fallback
        _uiState.update {
            it.copy(
                leaderboard = effectiveLb.take(10),
                currentUserId = profile.name,
                isOfflineMode = isOffline
            )
        }
    }

    fun loadPracticeData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val subjects = questionBank.getSubjects()
                val unitsMap = subjects.associate { it.id to questionBank.getUnits(it.id) }
                val unitsCountMap = subjects.associate { it.id to (unitsMap[it.id]?.size ?: 0) }
                val quizzesCountMap = subjects.associate { it.id to it.quizCount }

                val completedMap = mutableMapOf<String, Int>()
                runCatching {
                    val subjectProgressList = progressRepository.getAllSubjectProgress()
                    subjectProgressList.forEach { sp ->
                        completedMap[sp.subjectId] = sp.completedUnits
                    }
                }

                val initialSubjectId = _uiState.value.selectedSubjectId ?: subjects.firstOrNull()?.id ?: "math"
                val initialUnits = unitsMap[initialSubjectId].orEmpty()
                val recs = runCatching { progressRepository.getRecommendations() }.getOrDefault(emptyList())
                val weak = runCatching { progressRepository.getWeakAreas(10) }.getOrDefault(emptyList())
                val openMistakes = mistakeRepository.openMistakes.value.size

                val profile = profileRepository?.userProfile?.value ?: UserProfile()
                val fallback = LeaderboardRepository().global(profile)
                val cloudLb = syncRepository?.leaderboard?.value.orEmpty()
                val isOffline = cloudLb.isEmpty()
                val effectiveLb = if (cloudLb.isNotEmpty()) cloudLb else fallback

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subjects = subjects,
                        subjectUnitsCountMap = unitsCountMap,
                        subjectQuizzesCountMap = quizzesCountMap,
                        subjectUnitsMap = unitsMap,
                        subjectCompletedUnitsMap = completedMap,
                        leaderboard = effectiveLb.take(10),
                        currentUserId = profile.name,
                        isOfflineMode = isOffline,
                        expandedSubjectId = it.expandedSubjectId,
                        selectedSubjectId = initialSubjectId,
                        subjectUnits = initialUnits,
                        recommendations = recs,
                        weakAreas = weak,
                        openMistakesCount = openMistakes,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load subjects") }
            }
        }
    }

    fun toggleSubject(subjectId: String) {
        _uiState.update { current ->
            val next = if (current.expandedSubjectId == subjectId) null else subjectId
            current.copy(
                expandedSubjectId = next,
                selectedSubjectId = next ?: current.selectedSubjectId
            )
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
                expandedSubjectId = subjectId,
                subjectUnits = units
            )
        }
    }

    companion object {
        fun provideFactory(
            questionBank: QuestionBank,
            progressRepository: IProgressRepository,
            quizRepository: IQuizRepository,
            mistakeRepository: IMistakeRepository,
            syncRepository: SyncRepository? = null,
            profileRepository: IProfileRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PracticeViewModel(
                    questionBank,
                    progressRepository,
                    quizRepository,
                    mistakeRepository,
                    syncRepository,
                    profileRepository
                ) as T
            }
        }
    }
}
