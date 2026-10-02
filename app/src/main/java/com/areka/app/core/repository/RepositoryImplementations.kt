package com.areka.app.core.repository

import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.sync.SyncRepository
import com.areka.app.data.local.AppDatabase
import com.areka.app.data.local.CardStatus
import com.areka.app.data.local.FlashcardProgressEntity
import com.areka.app.data.local.FlashcardScheduleEntity
import com.areka.app.data.local.MistakeEntity
import com.areka.app.data.local.QuizAttemptEntity
import com.areka.app.data.local.RecentActivityEntity
import com.areka.app.data.local.ReviewGrade
import com.areka.app.data.local.UserProfileEntity
import com.areka.app.data.local.currentOwnerId
import com.areka.app.data.local.ownerIdFlow
import com.areka.app.data.model.Flashcard
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScore
import com.areka.app.data.model.RecentActivity
import com.areka.app.data.model.SubjectProgress
import com.areka.app.data.model.UnitProgress
import com.areka.app.data.model.UserProfile
import com.areka.app.data.repository.FlashcardScheduler
import com.areka.app.data.repository.StudyStreakCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AppQuizRepository(
    private val databaseProvider: () -> AppDatabase?,
    private val questionBank: QuestionBank,
    private val syncRepository: SyncRepository,
    private val scope: CoroutineScope
) : IQuizRepository {

    private val _recentActivities = MutableStateFlow<List<RecentActivity>>(emptyList())

    init {
        scope.launch {
            ownerIdFlow().flatMapLatest { owner ->
                databaseProvider()?.recentActivityDao()?.getRecentActivities(owner) ?: flowOf(emptyList())
            }.collectLatest { list ->
                _recentActivities.value = list.map { it.toRecentActivity() }
            }
        }
    }

    override suspend fun getQuizForUnit(unitId: String): Quiz = withContext(Dispatchers.IO) {
        questionBank.getQuizForUnit(unitId)
    }

    override fun getAllQuizzes(): List<Quiz> = questionBank.getAllQuizzes()

    override suspend fun getUnitProgress(unitId: String): UnitProgress = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext UnitProgress(unitId, 0, 0, 0, 0, null)
        val owner = currentOwnerId()
        val attempts = db.studyDao().getAttemptsForUnit(owner, unitId)
        val schedules = db.flashcardScheduleDao().getSchedulesForUnitOnce(owner, unitId)
        val reviewed = schedules.count { it.repetitions > 0 || it.lastReviewedAtEpochMillis != null }
        val accuracy = if (attempts.isEmpty()) 0 else {
            val correct = attempts.sumOf { it.correctAnswers }
            val total = attempts.sumOf { it.totalQuestions }.coerceAtLeast(1)
            ((correct.toFloat() / total) * 100).toInt()
        }
        val mastery = ((accuracy * 0.7f) + (reviewed.coerceAtMost(10) / 10f * 30f)).toInt().coerceIn(0, 100)
        UnitProgress(
            unitId = unitId,
            quizAttempts = attempts.size,
            quizAccuracyPercent = accuracy,
            flashcardsReviewed = reviewed,
            masteryPercent = mastery,
            lastStudiedAtEpochMillis = attempts.maxOfOrNull { it.completedAtEpochMillis }
        )
    }

    override suspend fun recordQuizResult(
        quiz: Quiz,
        score: QuizScore,
        timeSpentSeconds: Int,
        mistakes: List<MistakeEntity>
    ): QuizAttemptEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val owner = currentOwnerId()
        val attemptId = "attempt_${quiz.id}_${owner}_$now"
        val attempt = QuizAttemptEntity(
            ownerUserId = owner,
            id = attemptId,
            quizId = quiz.id,
            quizTitle = quiz.title,
            subjectId = quiz.subjectId ?: "",
            unitId = quiz.unitId ?: "",
            scorePercent = score.percentage,
            correctAnswers = score.correctAnswers,
            totalQuestions = score.totalQuestions,
            timeSpentSeconds = timeSpentSeconds.coerceAtLeast(0),
            completedAtEpochMillis = now
        )

        val db = databaseProvider()
        if (db != null) {
            db.studyDao().insertAttempt(attempt)
            if (mistakes.isNotEmpty()) {
                db.studyDao().insertMistakes(mistakes.map { it.copy(ownerUserId = owner) })
            }

            // Update user profile statistics deterministically
            val profile = db.userProfileDao().getUserProfileOnce(owner) ?: UserProfileEntity.default(owner)
            val allAttempts = db.studyDao().getAttemptsOnce(owner)
            val totalQuizzes = allAttempts.size
            val averageScore = if (allAttempts.isEmpty()) 0 else (allAttempts.sumOf { it.scorePercent } / totalQuizzes)
            val todayEpochDay = LocalDate.now().toEpochDay()
            val nextStreak = StudyStreakCalculator.nextStreak(profile.streakDays, profile.lastActiveDateEpochDay, todayEpochDay)
            val newTotalPoints = profile.totalPoints + score.pointsEarned
            val newHours = profile.timeStudiedHours + (timeSpentSeconds / 3600)

            val updatedProfile = profile.copy(
                streakDays = nextStreak,
                totalQuizzes = totalQuizzes,
                averageScore = averageScore,
                totalPoints = newTotalPoints,
                timeStudiedHours = newHours,
                lastActiveDateEpochDay = todayEpochDay
            )
            db.userProfileDao().insertOrUpdate(updatedProfile)

            // Insert recent activity
            val activity = RecentActivityEntity(
                ownerUserId = owner,
                id = "act_$now",
                title = "Completed Quiz",
                subtitle = "${quiz.title} • ${score.percentage}%",
                progressPercent = score.percentage,
                isCompleted = true,
                iconType = quiz.iconName,
                timestamp = now
            )
            db.recentActivityDao().insert(activity)

            // Trigger idempotent sync in background
            scope.launch {
                syncRepository.recordQuizAttempt(
                    quiz = quiz,
                    score = score,
                    completedAtIso = java.time.Instant.ofEpochMilli(now).toString(),
                    localAttemptId = attemptId
                )
                syncRepository.syncProfile(updatedProfile.toUserProfile())
            }
        }

        attempt
    }

    override fun observeRecentActivities(): Flow<List<RecentActivity>> = _recentActivities.asStateFlow()

    override suspend fun getAttemptsForUnit(unitId: String): List<QuizAttemptEntity> = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext emptyList()
        db.studyDao().getAttemptsForUnit(currentOwnerId(), unitId)
    }

    override fun observeAllAttempts(): Flow<List<QuizAttemptEntity>> {
        return ownerIdFlow().flatMapLatest { owner ->
            databaseProvider()?.studyDao()?.getAttempts(owner) ?: flowOf(emptyList())
        }
    }
}

class AppProgressRepository(
    private val databaseProvider: () -> AppDatabase?,
    private val questionBank: QuestionBank,
    private val profileRepository: IProfileRepository,
    private val mistakeRepository: IMistakeRepository,
    private val flashcardRepository: IFlashcardRepository
) : IProgressRepository {

    override fun observeUserProfile(): StateFlow<UserProfile> = profileRepository.userProfile

    override suspend fun getOverallProgress(): OverallStudyProgress = withContext(Dispatchers.IO) {
        val db = databaseProvider()
        val owner = currentOwnerId()
        val profile = profileRepository.userProfile.value
        val attempts = db?.studyDao()?.getAttemptsOnce(owner) ?: emptyList()
        val schedules = db?.flashcardScheduleDao()?.getSchedulesForOwner(owner) ?: emptyList()
        val masteredCards = schedules.count { it.status == CardStatus.REVIEW.name }
        val openMistakesCount = mistakeRepository.openMistakes.value.size

        val avgScore = if (attempts.isEmpty()) 0 else attempts.sumOf { it.scorePercent } / attempts.size

        OverallStudyProgress(
            completedQuizzes = attempts.size,
            averageScorePercent = avgScore,
            totalTimeStudiedHours = profile.timeStudiedHours,
            streakDays = profile.streakDays,
            totalPoints = profile.totalPoints,
            totalCardsMastered = masteredCards,
            openMistakesCount = openMistakesCount
        )
    }

    override suspend fun getSubjectProgress(subjectId: String): SubjectProgress = withContext(Dispatchers.IO) {
        val db = databaseProvider()
        val owner = currentOwnerId()
        val subject = questionBank.getSubject(subjectId) ?: return@withContext SubjectProgress(
            subjectId = subjectId,
            subjectName = subjectId,
            iconType = "quiz",
            accentColorHex = 0xFF1E40AF,
            completedUnits = 0,
            totalUnits = 0,
            averageScorePercent = 0,
            masteryPercent = 0,
            lastStudiedAtEpochMillis = null
        )

        val units = questionBank.getUnits(subjectId)
        val attempts = db?.studyDao()?.getAttemptsForSubject(owner, subjectId) ?: emptyList()

        val completedUnitIds = attempts.filter { it.scorePercent >= 70 }.map { it.unitId }.distinct()
        val completedUnits = completedUnitIds.size
        val avgScore = if (attempts.isEmpty()) 0 else attempts.sumOf { it.scorePercent } / attempts.size
        val mastery = if (units.isEmpty()) 0 else (completedUnits * 100 / units.size).coerceIn(0, 100)
        val lastStudied = attempts.maxOfOrNull { it.completedAtEpochMillis }

        SubjectProgress(
            subjectId = subject.id,
            subjectName = subject.name,
            iconType = subject.iconType,
            accentColorHex = subject.accentColorHex,
            completedUnits = completedUnits,
            totalUnits = units.size,
            averageScorePercent = avgScore,
            masteryPercent = mastery,
            lastStudiedAtEpochMillis = lastStudied
        )
    }

    override suspend fun getAllSubjectProgress(): List<SubjectProgress> = withContext(Dispatchers.IO) {
        questionBank.getSubjects().map { getSubjectProgress(it.id) }
    }

    override suspend fun getWeakAreas(limit: Int): List<WeakArea> = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext emptyList()
        val owner = currentOwnerId()
        val attempts = db.studyDao().getAttemptsOnce(owner)
        val mistakes = db.studyDao().getOpenMistakesOnce(owner)

        val unitMistakeCounts = mistakes.groupBy { it.unitId }
        val unitAttempts = attempts.groupBy { it.unitId }

        val weakList = mutableListOf<WeakArea>()

        unitAttempts.forEach { (unitId, atts) ->
            val correct = atts.sumOf { it.correctAnswers }
            val total = atts.sumOf { it.totalQuestions }.coerceAtLeast(1)
            val acc = ((correct.toFloat() / total) * 100).toInt()
            val mCount = unitMistakeCounts[unitId]?.size ?: 0
            if (acc < 75 || mCount > 0) {
                val unit = questionBank.getUnit(unitId)
                val subject = unit?.let { questionBank.getSubject(it.subjectId) }
                if (unit != null && subject != null) {
                    weakList.add(
                        WeakArea(
                            unitId = unit.id,
                            subjectId = subject.id,
                            unitTitle = unit.title,
                            subjectName = subject.name,
                            accuracyPercent = acc,
                            mistakeCount = mCount
                        )
                    )
                }
            }
        }

        // Also check any units that have open mistakes even if no recent attempt
        unitMistakeCounts.forEach { (unitId, mList) ->
            if (weakList.none { it.unitId == unitId }) {
                val unit = questionBank.getUnit(unitId)
                val subject = unit?.let { questionBank.getSubject(it.subjectId) }
                if (unit != null && subject != null) {
                    weakList.add(
                        WeakArea(
                            unitId = unit.id,
                            subjectId = subject.id,
                            unitTitle = unit.title,
                            subjectName = subject.name,
                            accuracyPercent = 0,
                            mistakeCount = mList.size
                        )
                    )
                }
            }
        }

        weakList.sortedWith(compareBy<WeakArea> { it.accuracyPercent }.thenByDescending { it.mistakeCount })
            .take(limit)
    }

    override suspend fun getRecommendations(): List<StudyRecommendation> = withContext(Dispatchers.IO) {
        val recs = mutableListOf<StudyRecommendation>()
        val openMistakes = mistakeRepository.openMistakes.value
        if (openMistakes.isNotEmpty()) {
            val topMistake = openMistakes.first()
            val unit = questionBank.getUnit(topMistake.unitId)
            recs.add(
                StudyRecommendation(
                    id = "rec_mistakes",
                    title = "Review ${openMistakes.size} Mistakes",
                    subtitle = unit?.title ?: "Past Quiz Weak Points",
                    subjectId = topMistake.subjectId,
                    unitId = topMistake.unitId,
                    actionType = ActionType.REVIEW_MISTAKES,
                    reason = "Fix concepts you previously missed"
                )
            )
        }

        val dueCards = flashcardRepository.getDueCount()
        if (dueCards > 0) {
            val allUnits = questionBank.getSubjects().flatMap { questionBank.getUnits(it.id) }
            val firstUnitWithCards = allUnits.firstOrNull { questionBank.getFlashcards(it.id).isNotEmpty() }
            if (firstUnitWithCards != null) {
                recs.add(
                    StudyRecommendation(
                        id = "rec_flashcards",
                        title = "$dueCards Flashcards Due",
                        subtitle = "${firstUnitWithCards.title} Spaced Review",
                        subjectId = firstUnitWithCards.subjectId,
                        unitId = firstUnitWithCards.id,
                        actionType = ActionType.REVIEW_FLASHCARDS,
                        reason = "Scheduled for retention"
                    )
                )
            }
        }

        val weak = getWeakAreas(limit = 1)
        if (weak.isNotEmpty()) {
            val w = weak.first()
            recs.add(
                StudyRecommendation(
                    id = "rec_weak_${w.unitId}",
                    title = "Practice ${w.subjectName} Unit",
                    subtitle = w.unitTitle,
                    subjectId = w.subjectId,
                    unitId = w.unitId,
                    actionType = ActionType.PRACTICE_QUIZ,
                    reason = "Recent accuracy was ${w.accuracyPercent}%"
                )
            )
        }

        if (recs.isEmpty()) {
            val firstSubject = questionBank.getSubjects().firstOrNull()
            val firstUnit = firstSubject?.let { questionBank.getUnits(it.id).firstOrNull() }
            if (firstSubject != null && firstUnit != null) {
                recs.add(
                    StudyRecommendation(
                        id = "rec_start",
                        title = "Start ${firstSubject.name}",
                        subtitle = "Unit 1: ${firstUnit.title}",
                        subjectId = firstSubject.id,
                        unitId = firstUnit.id,
                        actionType = ActionType.PRACTICE_QUIZ,
                        reason = "Begin your Grade 10 curriculum journey"
                    )
                )
            }
        }

        recs
    }
}

class AppFlashcardRepository(
    private val databaseProvider: () -> AppDatabase?,
    private val questionBank: QuestionBank,
    private val scope: CoroutineScope
) : IFlashcardRepository {

    private val _dueCount = MutableStateFlow(0)

    init {
        scope.launch {
            ownerIdFlow().flatMapLatest { owner ->
                databaseProvider()?.flashcardScheduleDao()?.observeDueCount(owner, System.currentTimeMillis())
                    ?: flowOf(0)
            }.collectLatest { count ->
                _dueCount.value = count
            }
        }
    }

    override suspend fun getDueCount(): Int = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext 0
        db.flashcardScheduleDao().countDueCards(currentOwnerId(), System.currentTimeMillis())
    }

    override fun observeDueCount(): StateFlow<Int> = _dueCount.asStateFlow()

    override suspend fun getCardsForUnit(unitId: String): List<Flashcard> = withContext(Dispatchers.IO) {
        questionBank.getFlashcards(unitId)
    }

    override suspend fun getSchedulesForUnit(unitId: String): List<FlashcardScheduleEntity> = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext emptyList()
        db.flashcardScheduleDao().getSchedulesForUnitOnce(currentOwnerId(), unitId)
    }

    override suspend fun gradeCard(
        cardId: String,
        subjectId: String,
        unitId: String,
        grade: ReviewGrade
    ): FlashcardScheduleEntity = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext FlashcardScheduleEntity(
            ownerUserId = currentOwnerId(),
            cardId = cardId,
            subjectId = subjectId,
            unitId = unitId,
            status = CardStatus.NEW.name
        )
        val owner = currentOwnerId()
        val currentSchedule = db.flashcardScheduleDao().getScheduleForCard(owner, cardId)
            ?: FlashcardScheduleEntity(
                ownerUserId = owner,
                cardId = cardId,
                subjectId = subjectId,
                unitId = unitId,
                status = CardStatus.NEW.name
            )

        val now = System.currentTimeMillis()
        val (newSchedule, log) = FlashcardScheduler.gradeCard(currentSchedule, grade, now)
        db.flashcardScheduleDao().insertOrUpdate(newSchedule.copy(ownerUserId = owner))
        db.flashcardScheduleDao().insertReviewLog(log.copy(ownerUserId = owner))

        // Update progress entity
        val isKnown = grade == ReviewGrade.GOOD || grade == ReviewGrade.EASY
        db.flashcardProgressDao().setCardProgress(
            FlashcardProgressEntity(
                ownerUserId = owner,
                cardId = cardId,
                unitId = unitId,
                isKnown = isKnown,
                updatedAt = now
            )
        )

        newSchedule
    }

    override suspend fun getNextIntervalPreview(
        schedule: FlashcardScheduleEntity,
        grade: ReviewGrade
    ): String = withContext(Dispatchers.IO) {
        FlashcardScheduler.getNextIntervalPreview(schedule, grade)
    }
}

class AppMistakeRepository(
    private val databaseProvider: () -> AppDatabase?,
    private val scope: CoroutineScope
) : IMistakeRepository {

    private val _openMistakes = MutableStateFlow<List<MistakeEntity>>(emptyList())
    override val openMistakes: StateFlow<List<MistakeEntity>> = _openMistakes.asStateFlow()

    init {
        scope.launch {
            ownerIdFlow().flatMapLatest { owner ->
                databaseProvider()?.studyDao()?.getOpenMistakes(owner)
                    ?: flowOf(emptyList())
            }.collectLatest { list ->
                _openMistakes.value = list
            }
        }
    }

    override suspend fun getMistakesForUnit(unitId: String): List<MistakeEntity> = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext emptyList()
        db.studyDao().getMistakesForUnit(currentOwnerId(), unitId)
    }

    override suspend fun markReviewed(quizId: String, questionId: Int) = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext
        db.studyDao().markMistakeReviewed(currentOwnerId(), quizId, questionId, System.currentTimeMillis())
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        val db = databaseProvider() ?: return@withContext
        db.studyDao().markAllMistakesReviewed(currentOwnerId(), System.currentTimeMillis())
    }
}

class AppProfileRepository(
    private val databaseProvider: () -> AppDatabase?,
    private val syncRepository: SyncRepository,
    private val scope: CoroutineScope
) : IProfileRepository {

    private val _userProfile = MutableStateFlow(UserProfile())
    override val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    override val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        scope.launch {
            ownerIdFlow().flatMapLatest { owner ->
                databaseProvider()?.userProfileDao()?.getUserProfile(owner)
                    ?: flowOf(null)
            }.collectLatest { entity ->
                val profile = entity?.toUserProfile() ?: UserProfile()
                _userProfile.value = profile
                _isDarkTheme.value = entity?.isDarkTheme ?: false
            }
        }
    }

    override fun updateProfile(name: String, grade: String) {
        scope.launch(Dispatchers.IO) {
            val db = databaseProvider() ?: return@launch
            val owner = currentOwnerId()
            val current = db.userProfileDao().getUserProfileOnce(owner) ?: UserProfileEntity.default(owner)
            val updated = current.copy(name = name.trim(), grade = grade.trim())
            db.userProfileDao().insertOrUpdate(updated)
            syncRepository.syncProfile(updated.toUserProfile())
        }
    }

    override fun toggleTheme() {
        scope.launch(Dispatchers.IO) {
            val db = databaseProvider() ?: return@launch
            val owner = currentOwnerId()
            val current = db.userProfileDao().getUserProfileOnce(owner) ?: UserProfileEntity.default(owner)
            val updated = current.copy(isDarkTheme = !current.isDarkTheme)
            db.userProfileDao().insertOrUpdate(updated)
            _isDarkTheme.value = updated.isDarkTheme
        }
    }
}
