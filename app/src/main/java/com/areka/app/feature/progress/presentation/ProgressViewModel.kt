package com.areka.app.feature.progress.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.repository.IProfileRepository
import com.areka.app.core.repository.IProgressRepository
import com.areka.app.core.sync.SyncRepository
import com.areka.app.data.model.Achievement
import com.areka.app.data.model.DailyStreakBadge
import com.areka.app.data.remote.AuthState
import com.areka.app.data.remote.SupabaseAuth
import com.areka.app.data.repository.AchievementCalculator
import com.areka.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val progressRepository: IProgressRepository,
    private val profileRepository: IProfileRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            profileRepository.userProfile.collect { profile ->
                _uiState.update {
                    it.copy(
                        profile = profile,
                        streakBadges = StudyRepository.getStreakBadges(profile.streakDays),
                        achievements = AchievementCalculator.calculate(profile)
                    )
                }
            }
        }
        viewModelScope.launch {
            syncRepository.leaderboard.collect { lb ->
                _uiState.update { it.copy(leaderboard = lb) }
            }
        }
    }

    fun loadProgress() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val overall = progressRepository.getOverallProgress()
                val subjects = progressRepository.getAllSubjectProgress()
                val profile = profileRepository.userProfile.value
                val streakBadges = StudyRepository.getStreakBadges(profile.streakDays)
                val achievements = AchievementCalculator.calculate(profile)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        overall = overall,
                        subjectProgressList = subjects,
                        profile = profile,
                        streakBadges = streakBadges,
                        achievements = achievements,
                        error = null
                    )
                }

                // If authenticated, refresh remote leaderboard
                if (SupabaseAuth.state.value is AuthState.SignedIn) {
                    syncRepository.refreshLeaderboard()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load progress") }
            }
        }
    }

    companion object {
        fun provideFactory(
            progressRepository: IProgressRepository,
            profileRepository: IProfileRepository,
            syncRepository: SyncRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel(progressRepository, profileRepository, syncRepository) as T
            }
        }
    }
}
