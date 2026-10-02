package com.areka.app.core.repository

import com.areka.app.data.local.FlashcardScheduleEntity
import com.areka.app.data.local.MistakeEntity
import com.areka.app.data.local.QuizAttemptEntity
import com.areka.app.data.local.ReviewGrade
import com.areka.app.data.model.Flashcard
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScore
import com.areka.app.data.model.RecentActivity
import com.areka.app.data.model.SubjectItem
import com.areka.app.data.model.SubjectProgress
import com.areka.app.data.model.SubjectUnit
import com.areka.app.data.model.UnitProgress
import com.areka.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class OverallStudyProgress(
    val completedQuizzes: Int,
    val averageScorePercent: Int,
    val totalTimeStudiedHours: Int,
    val streakDays: Int,
    val totalPoints: Int,
    val totalCardsMastered: Int,
    val openMistakesCount: Int
)

data class WeakArea(
    val unitId: String,
    val subjectId: String,
    val unitTitle: String,
    val subjectName: String,
    val accuracyPercent: Int,
    val mistakeCount: Int
)

data class StudyRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val subjectId: String,
    val unitId: String,
    val actionType: ActionType,
    val reason: String
)

enum class ActionType {
    PRACTICE_QUIZ,
    REVIEW_FLASHCARDS,
    REVIEW_MISTAKES
}

interface IQuizRepository {
    suspend fun getQuizForUnit(unitId: String): Quiz
    fun getAllQuizzes(): List<Quiz>
    suspend fun getUnitProgress(unitId: String): UnitProgress
    suspend fun recordQuizResult(
        quiz: Quiz,
        score: QuizScore,
        timeSpentSeconds: Int,
        mistakes: List<MistakeEntity>
    ): QuizAttemptEntity
    fun observeRecentActivities(): Flow<List<RecentActivity>>
    suspend fun getAttemptsForUnit(unitId: String): List<QuizAttemptEntity>
    fun observeAllAttempts(): Flow<List<QuizAttemptEntity>>
}

interface IProgressRepository {
    fun observeUserProfile(): StateFlow<UserProfile>
    suspend fun getOverallProgress(): OverallStudyProgress
    suspend fun getSubjectProgress(subjectId: String): SubjectProgress
    suspend fun getAllSubjectProgress(): List<SubjectProgress>
    suspend fun getWeakAreas(limit: Int = 3): List<WeakArea>
    suspend fun getRecommendations(): List<StudyRecommendation>
}

interface IFlashcardRepository {
    suspend fun getDueCount(): Int
    fun observeDueCount(): StateFlow<Int>
    suspend fun getCardsForUnit(unitId: String): List<Flashcard>
    suspend fun getSchedulesForUnit(unitId: String): List<FlashcardScheduleEntity>
    suspend fun gradeCard(
        cardId: String,
        subjectId: String,
        unitId: String,
        grade: ReviewGrade
    ): FlashcardScheduleEntity
    suspend fun getNextIntervalPreview(schedule: FlashcardScheduleEntity, grade: ReviewGrade): String
}

interface IMistakeRepository {
    val openMistakes: StateFlow<List<MistakeEntity>>
    suspend fun getMistakesForUnit(unitId: String): List<MistakeEntity>
    suspend fun markReviewed(quizId: String, questionId: Int)
    suspend fun clearAll()
}

interface IProfileRepository {
    val userProfile: StateFlow<UserProfile>
    val isDarkTheme: StateFlow<Boolean>
    fun updateProfile(name: String, grade: String)
    fun toggleTheme()
}
