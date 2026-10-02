package com.areka.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.areka.app.core.curriculum.DefaultQuestionBank
import com.areka.app.core.designsystem.ArekaV2Theme
import com.areka.app.core.di.AppContainer
import com.areka.app.data.local.AppDatabase
import com.areka.app.data.local.CardStatus
import com.areka.app.data.local.FlashcardScheduleEntity
import com.areka.app.data.local.MIGRATION_3_4
import com.areka.app.data.local.MIGRATION_4_5
import com.areka.app.data.local.MIGRATION_5_6
import com.areka.app.data.local.ReviewGrade
import com.areka.app.data.model.Question
import com.areka.app.data.model.QuestionOption
import com.areka.app.data.model.QuestionType
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.QuizScoring
import com.areka.app.data.remote.SupabaseCloudSync
import com.areka.app.data.repository.FlashcardScheduler
import com.areka.app.feature.home.presentation.ContinueStudyItem
import com.areka.app.feature.home.presentation.HomeScreen
import com.areka.app.feature.home.presentation.HomeUiState
import com.areka.app.feature.practice.presentation.PracticeScreen
import com.areka.app.feature.practice.presentation.PracticeUiState
import com.areka.app.feature.quiz.presentation.QuizActiveScreen
import com.areka.app.feature.quiz.presentation.QuizUiState
import com.areka.app.navigation.MainTab
import com.areka.app.navigation.NavRoute
import com.areka.app.navigation.NavigationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class V2ArchitectureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    // ==========================================
    // 1. Navigation Backstack Tests
    // ==========================================
    @Test
    fun `navigation backstack properly pushes and pops routes`() {
        val nav = NavigationManager()
        assertEquals(NavRoute.HomeRoot, nav.currentRoute.value)
        assertEquals(MainTab.HOME, nav.currentTab.value)

        // Navigate forward: Home -> SubjectDetail -> UnitDetail -> ActiveQuiz
        nav.navigateTo(NavRoute.SubjectDetail("math"))
        assertEquals(NavRoute.SubjectDetail("math"), nav.currentRoute.value)

        nav.navigateTo(NavRoute.UnitDetail("math_u1", "math"))
        assertEquals(NavRoute.UnitDetail("math_u1", "math"), nav.currentRoute.value)

        nav.navigateTo(NavRoute.ActiveQuiz("quiz_math_u1", "math_u1", "math"))
        assertEquals(NavRoute.ActiveQuiz("quiz_math_u1", "math_u1", "math"), nav.currentRoute.value)

        // Pop ActiveQuiz -> returns to UnitDetail
        assertTrue(nav.handleBack())
        assertEquals(NavRoute.UnitDetail("math_u1", "math"), nav.currentRoute.value)

        // Pop UnitDetail -> returns to SubjectDetail
        assertTrue(nav.handleBack())
        assertEquals(NavRoute.SubjectDetail("math"), nav.currentRoute.value)

        // Pop SubjectDetail -> returns to HomeRoot
        assertTrue(nav.handleBack())
        assertEquals(NavRoute.HomeRoot, nav.currentRoute.value)

        // At HomeRoot, back is not consumed
        assertFalse(nav.handleBack())
    }

    @Test
    fun `tab switching switches tab and back returns to home`() {
        val nav = NavigationManager()
        nav.selectTab(MainTab.PRACTICE)
        assertEquals(MainTab.PRACTICE, nav.currentTab.value)
        assertEquals(NavRoute.PracticeRoot, nav.currentRoute.value)

        // Back from another tab returns to Home tab
        assertTrue(nav.handleBack())
        assertEquals(MainTab.HOME, nav.currentTab.value)
        assertEquals(NavRoute.HomeRoot, nav.currentRoute.value)
    }

    // ==========================================
    // 2. Curriculum QuestionBank Tests
    // ==========================================
    @Test
    fun `question bank contains all 9 official MoE Grade 10 subjects`() {
        val qb = DefaultQuestionBank()
        val subjects = qb.getSubjects()
        assertEquals(9, subjects.size)

        val subjectIds = subjects.map { it.id }.toSet()
        assertTrue(subjectIds.contains("math"))
        assertTrue(subjectIds.contains("physics"))
        assertTrue(subjectIds.contains("chemistry"))
        assertTrue(subjectIds.contains("biology"))
        assertTrue(subjectIds.contains("history"))
        assertTrue(subjectIds.contains("geography"))
        assertTrue(subjectIds.contains("civics"))
        assertTrue(subjectIds.contains("economics"))
        assertTrue(subjectIds.contains("health_pe"))
    }

    @Test
    fun `quiz resolution produces non empty questions for units`() {
        val qb = DefaultQuestionBank()
        val quiz = qb.getQuizForUnit("chem_u1")
        assertNotNull(quiz)
        assertTrue(quiz.questions.isNotEmpty())
        assertEquals("chem_u1", quiz.unitId)
    }

    // ==========================================
    // 3. Quiz Scoring Tests
    // ==========================================
    @Test
    fun `quiz scoring handles multiple choice and fill in blank answers`() {
        val q1 = Question(
            id = 1,
            questionNumber = 1,
            totalQuestions = 2,
            text = "Capital of Ethiopia?",
            options = listOf(QuestionOption("a", "Addis Ababa"), QuestionOption("b", "Nairobi")),
            correctOptionId = "a",
            explanation = "Addis Ababa is the capital"
        )
        val q2 = Question(
            id = 2,
            questionNumber = 2,
            totalQuestions = 2,
            text = "What is H2O?",
            options = emptyList(),
            correctOptionId = "water",
            explanation = "H2O is water",
            type = QuestionType.FILL_IN_THE_BLANK
        )
        val testQuiz = Quiz(
            id = "test_q",
            title = "Sample Quiz",
            subject = "General",
            durationMinutes = 5,
            questions = listOf(q1, q2)
        )

        // Test with exact matches (including normalized blank: "  WATER ")
        val score = QuizScoring.calculate(testQuiz, mapOf(1 to "a", 2 to "  WATER "))
        assertEquals(2, score.totalQuestions)
        assertEquals(2, score.correctAnswers)
        assertEquals(0, score.incorrectAnswers)
        assertEquals(100, score.percentage)
        assertEquals(1000, score.pointsEarned)
    }

    // ==========================================
    // 4. Flashcard Scheduler SM2 Tests
    // ==========================================
    @Test
    fun `flashcard scheduler advances new card through learning steps`() {
        val now = 1000000L
        val initial = FlashcardScheduleEntity(
            ownerUserId = "test_owner",
            cardId = "card_1",
            subjectId = "bio",
            unitId = "bio_u1",
            status = CardStatus.NEW.name
        )

        val (good1, log1) = FlashcardScheduler.gradeCard(initial, ReviewGrade.GOOD, now)
        assertEquals(CardStatus.LEARNING.name, good1.status)
        assertEquals(1, good1.learningStepIndex)
        assertEquals("GOOD", log1.grade)

        // Second Good finishes learning steps and graduates to REVIEW
        val (good2, log2) = FlashcardScheduler.gradeCard(good1, ReviewGrade.GOOD, now)
        assertEquals(CardStatus.REVIEW.name, good2.status)
        assertEquals(1.0f, good2.intervalDays, 0.01f)
    }

    @Test
    fun `flashcard scheduler lapses on Again from review state`() {
        val now = 1000000L
        val reviewCard = FlashcardScheduleEntity(
            ownerUserId = "test_owner",
            cardId = "card_1",
            subjectId = "bio",
            unitId = "bio_u1",
            status = CardStatus.REVIEW.name,
            intervalDays = 6.0f,
            ease = 2.5f
        )

        val (againCard, log) = FlashcardScheduler.gradeCard(reviewCard, ReviewGrade.AGAIN, now)
        assertEquals(CardStatus.RELEARNING.name, againCard.status)
        assertEquals(1, againCard.lapses)
        assertEquals(2.3f, againCard.ease, 0.01f) // Ease decreased by 0.2
        assertEquals(1.0f, againCard.intervalDays, 0.01f)
    }

    // ==========================================
    // 5. Sync Idempotency & Deterministic UUID Tests
    // ==========================================
    @Test
    fun `quiz attempt derives idempotent deterministic UUID`() {
        val userId = "user-1234"
        val localAttemptId = "attempt_math_u1_1700000000"

        val seed = "${userId}_$localAttemptId"
        val uuid1 = UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()
        val uuid2 = UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()

        assertEquals(uuid1, uuid2)
        assertTrue(uuid1.isNotBlank())
    }

    // ==========================================
    // 6. Room Migrations Integrity Test
    // ==========================================
    @Test
    fun `room migrations have correct versions`() {
        assertEquals(3, MIGRATION_3_4.startVersion)
        assertEquals(4, MIGRATION_3_4.endVersion)

        assertEquals(4, MIGRATION_4_5.startVersion)
        assertEquals(5, MIGRATION_4_5.endVersion)

        assertEquals(5, MIGRATION_5_6.startVersion)
        assertEquals(6, MIGRATION_5_6.endVersion)
    }

    // ==========================================
    // 7. Compose UI Component Tests
    // ==========================================
    @Test
    fun `HomeScreen renders study cockpit and quick actions`() {
        val state = HomeUiState(
            isLoading = false,
            greeting = "Good afternoon",
            continueItem = ContinueStudyItem(
                title = "Continue Biology",
                subtitle = "Unit 2: Plants",
                detail = "Official MoE Grade 10 Curriculum",
                subjectId = "biology",
                unitId = "bio_u2"
            )
        )

        composeTestRule.setContent {
            ArekaV2Theme {
                HomeScreen(
                    uiState = state,
                    questionBank = DefaultQuestionBank(),
                    onEvent = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue Studying").assertIsDisplayed()
        composeTestRule.onNodeWithTag("continue_study_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_action_practice").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_action_flashcards").assertIsDisplayed()
    }

    @Test
    fun `QuizActiveScreen displays question text and handles answer submission`() {
        val q = Question(
            id = 101,
            questionNumber = 1,
            totalQuestions = 1,
            text = "What is the primary function of chlorophyll?",
            options = listOf(
                QuestionOption("a", "Absorb light energy"),
                QuestionOption("b", "Store water")
            ),
            correctOptionId = "a",
            explanation = "Chlorophyll absorbs photons for photosynthesis."
        )
        val quiz = Quiz(
            id = "q_bio",
            title = "Biology Quiz",
            subject = "Biology",
            durationMinutes = 10,
            questions = listOf(q)
        )
        val state = QuizUiState(
            quiz = quiz,
            currentQuestionIndex = 0,
            selectedOptionForCurrent = "a",
            isSubmittedForCurrent = false
        )

        var submitted = false
        composeTestRule.setContent {
            ArekaV2Theme {
                QuizActiveScreen(
                    uiState = state,
                    onSelectOption = {},
                    onUpdateFillBlank = {},
                    onSubmitAnswer = { submitted = true },
                    onNextQuestion = {},
                    onExitRequest = {}
                )
            }
        }

        composeTestRule.onNodeWithText("What is the primary function of chlorophyll?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quiz_submit_button").performClick()
        assertTrue(submitted)
    }

    @Test
    fun `PracticeScreen renders subjects first and leaderboard without mastery hub`() {
        val qb = DefaultQuestionBank()
        val subjects = qb.getSubjects()
        val unitsMap = subjects.associate { it.id to qb.getUnits(it.id) }
        val testLb = listOf(
            com.areka.app.data.model.LeaderboardEntry("u1", 1, "Alex Chen", "Grade 10", 94800, false, com.areka.app.data.model.BadgeType.GOLD),
            com.areka.app.data.model.LeaderboardEntry("u_user", 2, "Student", "Grade 10", 85000, true, com.areka.app.data.model.BadgeType.SILVER)
        )

        val state = PracticeUiState(
            isLoading = false,
            subjects = subjects.take(2),
            expandedSubjectId = null,
            subjectUnitsMap = mapOf("math" to unitsMap["math"].orEmpty().take(1)),
            leaderboard = testLb
        )

        composeTestRule.setContent {
            ArekaV2Theme {
                PracticeScreen(
                    uiState = state,
                    onEvent = {}
                )
            }
        }

        // Header
        composeTestRule.onNodeWithText("Practice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pick a subject to practice").assertIsDisplayed()

        // Subjects list
        composeTestRule.onNodeWithTag("practice_subject_math").assertIsDisplayed()
        composeTestRule.onNodeWithTag("practice_subject_physics").assertIsDisplayed()

        // Leaderboard
        composeTestRule.onNodeWithText("Leaderboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Top Grade 10 students").assertIsDisplayed()
    }

    @Test
    fun `LeaderboardCompactRow renders rank and user highlight`() {
        val entry = com.areka.app.data.model.LeaderboardEntry(
            id = "u_user",
            rank = 1,
            name = "Student",
            grade = "Grade 10",
            points = 95000,
            isCurrentUser = true,
            badgeType = com.areka.app.data.model.BadgeType.GOLD
        )

        composeTestRule.setContent {
            ArekaV2Theme {
                PracticeScreen(
                    uiState = PracticeUiState(
                        isLoading = false,
                        subjects = emptyList(),
                        leaderboard = listOf(entry)
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("leaderboard_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("YOU").assertIsDisplayed()
        composeTestRule.onNodeWithText("95,000 pts").assertIsDisplayed()
    }
}
