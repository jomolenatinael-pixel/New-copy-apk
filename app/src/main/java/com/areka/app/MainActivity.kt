package com.areka.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.areka.app.core.designsystem.ArekaV2Theme
import com.areka.app.core.di.AppContainer
import com.areka.app.data.local.MistakeEntity
import com.areka.app.data.remote.AuthState
import com.areka.app.data.remote.SupabaseAuth
import com.areka.app.feature.flashcards.presentation.FlashcardEvent
import com.areka.app.feature.flashcards.presentation.FlashcardStudyScreen
import com.areka.app.feature.flashcards.presentation.FlashcardViewModel
import com.areka.app.feature.home.presentation.HomeEvent
import com.areka.app.feature.home.presentation.HomeScreen
import com.areka.app.feature.home.presentation.HomeViewModel
import com.areka.app.feature.learn.presentation.LearnEvent
import com.areka.app.feature.learn.presentation.LearnScreen
import com.areka.app.feature.learn.presentation.LearnViewModel
import com.areka.app.feature.learn.presentation.SubjectDetailScreen
import com.areka.app.feature.learn.presentation.UnitDetailScreen
import com.areka.app.feature.practice.presentation.PracticeCategory
import com.areka.app.feature.practice.presentation.PracticeEvent
import com.areka.app.feature.practice.presentation.PracticeScreen
import com.areka.app.feature.practice.presentation.PracticeViewModel
import com.areka.app.feature.profile.presentation.MistakesReviewScreen
import com.areka.app.feature.profile.presentation.ProfileEvent
import com.areka.app.feature.profile.presentation.ProfileScreen
import com.areka.app.feature.profile.presentation.ProfileViewModel
import com.areka.app.feature.progress.presentation.ProgressEvent
import com.areka.app.feature.progress.presentation.ProgressScreen
import com.areka.app.feature.progress.presentation.ProgressViewModel
import com.areka.app.feature.quiz.presentation.QuizActiveScreen
import com.areka.app.feature.quiz.presentation.QuizResultScreen
import com.areka.app.feature.quiz.presentation.QuizViewModel
import com.areka.app.navigation.ArekaV2BottomBar
import com.areka.app.navigation.ArekaV2TopBar
import com.areka.app.navigation.MainTab
import com.areka.app.navigation.NavRoute
import com.areka.app.navigation.NavigationManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val navigationManager = NavigationManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = AppContainer.getInstance(this)
        handleAuthIntent(intent)

        setContent {
            ArekaV2App(appContainer = appContainer, navManager = navigationManager)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "areka" && data.host == "auth") {
            SupabaseAuth.handleRecoveryUri(data)
            navigationManager.selectTab(MainTab.YOU)
        }
    }
}

@Composable
fun ArekaV2App(
    appContainer: AppContainer,
    navManager: NavigationManager
) {
    val coroutineScope = rememberCoroutineScope()

    val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = HomeViewModel.provideFactory(
            profileRepository = appContainer.profileRepository,
            progressRepository = appContainer.progressRepository,
            quizRepository = appContainer.quizRepository,
            flashcardRepository = appContainer.flashcardRepository,
            mistakeRepository = appContainer.mistakeRepository,
            questionBank = appContainer.questionBank
        )
    )

    val learnViewModel: LearnViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = LearnViewModel.provideFactory(
            questionBank = appContainer.questionBank,
            progressRepository = appContainer.progressRepository,
            quizRepository = appContainer.quizRepository
        )
    )

    val practiceViewModel: PracticeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = PracticeViewModel.provideFactory(
            questionBank = appContainer.questionBank,
            progressRepository = appContainer.progressRepository,
            quizRepository = appContainer.quizRepository,
            mistakeRepository = appContainer.mistakeRepository,
            syncRepository = appContainer.syncRepository,
            profileRepository = appContainer.profileRepository
        )
    )

    val progressViewModel: ProgressViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ProgressViewModel.provideFactory(
            progressRepository = appContainer.progressRepository,
            profileRepository = appContainer.profileRepository,
            syncRepository = appContainer.syncRepository
        )
    )

    val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ProfileViewModel.provideFactory(
            profileRepository = appContainer.profileRepository,
            syncRepository = appContainer.syncRepository
        )
    )

    val quizViewModel: QuizViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = QuizViewModel.provideFactory(appContainer.quizRepository)
    )

    val flashcardViewModel: FlashcardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = FlashcardViewModel.provideFactory(
            flashcardRepository = appContainer.flashcardRepository,
            questionBank = appContainer.questionBank
        )
    )

    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val learnUiState by learnViewModel.uiState.collectAsStateWithLifecycle()
    val practiceUiState by practiceViewModel.uiState.collectAsStateWithLifecycle()
    val progressUiState by progressViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val quizUiState by quizViewModel.uiState.collectAsStateWithLifecycle()
    val flashcardUiState by flashcardViewModel.uiState.collectAsStateWithLifecycle()

    val currentTab by navManager.currentTab.collectAsStateWithLifecycle()
    val currentRoute by navManager.currentRoute.collectAsStateWithLifecycle()
    val isDarkTheme by profileViewModel.uiState.collectAsStateWithLifecycle().let {
        remember(it.value.isDarkTheme) { derivedStateOf { it.value.isDarkTheme } }
    }

    val openMistakes by appContainer.mistakeRepository.openMistakes.collectAsStateWithLifecycle()

    // Unidirectional back handling
    BackHandler(enabled = currentRoute !is NavRoute.HomeRoot || currentTab != MainTab.HOME) {
        navManager.handleBack()
    }

    ArekaV2Theme(darkTheme = isDarkTheme) {
        val showBottomNav = currentRoute is NavRoute.HomeRoot ||
                currentRoute is NavRoute.LearnRoot ||
                currentRoute is NavRoute.PracticeRoot ||
                currentRoute is NavRoute.ProgressRoot ||
                currentRoute is NavRoute.YouRoot

        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    ArekaV2BottomBar(
                        currentTab = currentTab,
                        onTabSelected = { tab -> navManager.selectTab(tab) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (showBottomNav) innerPadding.calculateBottomPadding() else androidx.compose.ui.unit.Dp(0f)
                    )
            ) {
                when (val route = currentRoute) {
                    is NavRoute.HomeRoot -> {
                        HomeScreen(
                            uiState = homeUiState,
                            questionBank = appContainer.questionBank,
                            onEvent = { event ->
                                when (event) {
                                    is HomeEvent.OpenSubject -> {
                                        learnViewModel.selectSubject(event.subjectId)
                                        navManager.navigateTo(NavRoute.SubjectDetail(event.subjectId))
                                    }
                                    is HomeEvent.OpenUnitQuiz -> {
                                        val quiz = appContainer.questionBank.getQuizForUnit(event.unitId)
                                        quizViewModel.loadQuiz(quiz)
                                        navManager.navigateTo(NavRoute.ActiveQuiz(quiz.id, event.unitId, event.subjectId))
                                    }
                                    is HomeEvent.OpenFlashcards -> {
                                        flashcardViewModel.loadDeck(event.unitId, event.subjectId)
                                        navManager.navigateTo(NavRoute.FlashcardStudy(event.unitId, event.subjectId))
                                    }
                                    is HomeEvent.OpenMistakes -> {
                                        navManager.navigateTo(NavRoute.MistakesReview(event.unitId))
                                    }
                                    is HomeEvent.QuickPractice -> {
                                        navManager.selectTab(MainTab.PRACTICE)
                                    }
                                    is HomeEvent.QuickFlashcards -> {
                                        val firstSubject = appContainer.questionBank.getSubjects().firstOrNull()
                                        if (firstSubject != null) {
                                            val firstUnit = appContainer.questionBank.getUnits(firstSubject.id).firstOrNull()
                                            if (firstUnit != null) {
                                                flashcardViewModel.loadDeck(firstUnit.id, firstSubject.id)
                                                navManager.navigateTo(NavRoute.FlashcardStudy(firstUnit.id, firstSubject.id))
                                            }
                                        }
                                    }
                                    is HomeEvent.QuickProgress -> {
                                        navManager.selectTab(MainTab.PROGRESS)
                                    }
                                    is HomeEvent.Refresh -> {
                                        homeViewModel.loadHomeData()
                                    }
                                }
                            }
                        )
                    }

                    is NavRoute.LearnRoot -> {
                        LearnScreen(
                            uiState = learnUiState,
                            questionBank = appContainer.questionBank,
                            onEvent = { event ->
                                when (event) {
                                    is LearnEvent.SearchQueryChanged -> learnViewModel.onSearchQueryChanged(event.query)
                                    is LearnEvent.SelectSubject -> {
                                        learnViewModel.selectSubject(event.subjectId)
                                        navManager.navigateTo(NavRoute.SubjectDetail(event.subjectId))
                                    }
                                    is LearnEvent.SelectUnit -> {
                                        learnViewModel.selectUnit(event.unitId, event.subjectId)
                                        navManager.navigateTo(NavRoute.UnitDetail(event.unitId, event.subjectId))
                                    }
                                    is LearnEvent.StartUnitQuiz -> {
                                        val quiz = appContainer.questionBank.getQuizForUnit(event.unitId)
                                        quizViewModel.loadQuiz(quiz)
                                        navManager.navigateTo(NavRoute.ActiveQuiz(quiz.id, event.unitId, event.subjectId))
                                    }
                                    is LearnEvent.StudyUnitFlashcards -> {
                                        flashcardViewModel.loadDeck(event.unitId, event.subjectId)
                                        navManager.navigateTo(NavRoute.FlashcardStudy(event.unitId, event.subjectId))
                                    }
                                    is LearnEvent.BackToSubjects -> navManager.handleBack()
                                    is LearnEvent.BackToUnits -> navManager.handleBack()
                                }
                            }
                        )
                    }

                    is NavRoute.SubjectDetail -> {
                        val subject = learnUiState.selectedSubject
                            ?: appContainer.questionBank.getSubject(route.subjectId)
                        if (subject != null) {
                            SubjectDetailScreen(
                                subject = subject,
                                progress = learnUiState.selectedSubjectProgress,
                                units = learnUiState.selectedSubjectUnits,
                                onUnitClick = { unitId ->
                                    learnViewModel.selectUnit(unitId, subject.id)
                                    navManager.navigateTo(NavRoute.UnitDetail(unitId, subject.id))
                                },
                                onQuizClick = { unitId ->
                                    val quiz = appContainer.questionBank.getQuizForUnit(unitId)
                                    quizViewModel.loadQuiz(quiz)
                                    navManager.navigateTo(NavRoute.ActiveQuiz(quiz.id, unitId, subject.id))
                                },
                                onFlashcardsClick = { unitId ->
                                    flashcardViewModel.loadDeck(unitId, subject.id)
                                    navManager.navigateTo(NavRoute.FlashcardStudy(unitId, subject.id))
                                },
                                onBack = { navManager.handleBack() }
                            )
                        } else {
                            navManager.handleBack()
                        }
                    }

                    is NavRoute.UnitDetail -> {
                        val unit = learnUiState.selectedUnit
                            ?: appContainer.questionBank.getUnit(route.unitId)
                        if (unit != null) {
                            UnitDetailScreen(
                                unit = unit,
                                progress = learnUiState.selectedUnitProgress,
                                flashcards = learnUiState.selectedUnitFlashcards,
                                onStartQuiz = {
                                    val quiz = appContainer.questionBank.getQuizForUnit(unit.id)
                                    quizViewModel.loadQuiz(quiz)
                                    navManager.navigateTo(NavRoute.ActiveQuiz(quiz.id, unit.id, route.subjectId))
                                },
                                onStudyFlashcards = {
                                    flashcardViewModel.loadDeck(unit.id, route.subjectId)
                                    navManager.navigateTo(NavRoute.FlashcardStudy(unit.id, route.subjectId))
                                },
                                onBack = { navManager.handleBack() }
                            )
                        } else {
                            navManager.handleBack()
                        }
                    }

                    is NavRoute.PracticeRoot -> {
                        PracticeScreen(
                            uiState = practiceUiState,
                            onEvent = { event ->
                                when (event) {
                                    is PracticeEvent.ToggleSubject -> practiceViewModel.toggleSubject(event.subjectId)
                                    is PracticeEvent.OpenSubject -> {
                                        learnViewModel.selectSubject(event.subjectId)
                                        navManager.navigateTo(NavRoute.SubjectDetail(event.subjectId))
                                    }
                                    is PracticeEvent.ViewFullLeaderboard -> {
                                        navManager.selectTab(MainTab.PROGRESS)
                                    }
                                    is PracticeEvent.SelectCategory -> practiceViewModel.selectCategory(event.category)
                                    is PracticeEvent.SelectSubjectFilter -> practiceViewModel.selectSubjectFilter(event.subjectId)
                                    is PracticeEvent.StartQuiz -> {
                                        val quiz = appContainer.questionBank.getQuiz(event.quizId)
                                            ?: appContainer.questionBank.getQuizForUnit(event.unitId)
                                        quizViewModel.loadQuiz(quiz)
                                        navManager.navigateTo(NavRoute.ActiveQuiz(quiz.id, event.unitId, event.subjectId))
                                    }
                                    is PracticeEvent.ReviewMistakes -> {
                                        navManager.navigateTo(NavRoute.MistakesReview(event.unitId))
                                    }
                                    is PracticeEvent.StudyFlashcards -> {
                                        flashcardViewModel.loadDeck(event.unitId, event.subjectId)
                                        navManager.navigateTo(NavRoute.FlashcardStudy(event.unitId, event.subjectId))
                                    }
                                    is PracticeEvent.Refresh -> practiceViewModel.loadPracticeData()
                                }
                            }
                        )
                    }

                    is NavRoute.ProgressRoot -> {
                        ProgressScreen(
                            uiState = progressUiState,
                            onEvent = { event ->
                                when (event) {
                                    is ProgressEvent.Refresh -> progressViewModel.loadProgress()
                                    is ProgressEvent.OpenSubject -> {
                                        learnViewModel.selectSubject(event.subjectId)
                                        navManager.navigateTo(NavRoute.SubjectDetail(event.subjectId))
                                    }
                                    is ProgressEvent.OpenMistakes -> {
                                        navManager.navigateTo(NavRoute.MistakesReview(null))
                                    }
                                }
                            }
                        )
                    }

                    is NavRoute.YouRoot, is NavRoute.Auth -> {
                        ProfileScreen(
                            uiState = profileUiState,
                            onEvent = { event ->
                                when (event) {
                                    is ProfileEvent.UpdateDisplayName -> profileViewModel.updateProfile(event.name, profileUiState.profile.grade)
                                    is ProfileEvent.UpdateGrade -> profileViewModel.updateProfile(profileUiState.profile.name, event.grade)
                                    is ProfileEvent.ToggleTheme -> profileViewModel.toggleTheme()
                                    is ProfileEvent.ManualSync -> profileViewModel.triggerManualSync()
                                    is ProfileEvent.EmailChanged -> profileViewModel.setEmail(event.email)
                                    is ProfileEvent.PasswordChanged -> profileViewModel.setPassword(event.pass)
                                    is ProfileEvent.ConfirmPasswordChanged -> profileViewModel.setConfirmPassword(event.pass)
                                    is ProfileEvent.DisplayNameChanged -> profileViewModel.setDisplayName(event.name)
                                    is ProfileEvent.NewPasswordInputChanged -> profileViewModel.setNewPasswordInput(event.pass)
                                    is ProfileEvent.SetSignUpMode -> profileViewModel.setSignUpMode(event.isSignUp)
                                    is ProfileEvent.SetForgotPasswordMode -> profileViewModel.setForgotPasswordMode(event.isForgot)
                                    is ProfileEvent.SubmitAuth -> profileViewModel.submitAuth()
                                    is ProfileEvent.SendRecoveryEmail -> profileViewModel.sendRecoveryEmail()
                                    is ProfileEvent.UpdatePasswordRecovery -> profileViewModel.updatePasswordRecovery()
                                    is ProfileEvent.SignOut -> profileViewModel.signOut()
                                }
                            }
                        )
                    }

                    is NavRoute.ActiveQuiz -> {
                        if (quizUiState.isCompleted) {
                            QuizResultScreen(
                                uiState = quizUiState,
                                onRetake = { quizViewModel.retakeQuiz() },
                                onReviewMistakes = {
                                    navManager.navigateTo(NavRoute.MistakesReview(route.unitId))
                                },
                                onBackToSubject = {
                                    homeViewModel.loadHomeData()
                                    navManager.handleBack()
                                }
                            )
                        } else {
                            QuizActiveScreen(
                                uiState = quizUiState,
                                onSelectOption = { quizViewModel.selectOption(it) },
                                onUpdateFillBlank = { quizViewModel.updateFillBlank(it) },
                                onSubmitAnswer = { quizViewModel.submitAnswer() },
                                onNextQuestion = { quizViewModel.nextQuestion() },
                                onExitRequest = { navManager.handleBack() }
                            )
                        }
                    }

                    is NavRoute.QuizResult -> {
                        QuizResultScreen(
                            uiState = quizUiState,
                            onRetake = { quizViewModel.retakeQuiz() },
                            onReviewMistakes = {
                                navManager.navigateTo(NavRoute.MistakesReview(route.quiz.unitId))
                            },
                            onBackToSubject = {
                                homeViewModel.loadHomeData()
                                navManager.handleBack()
                            }
                        )
                    }

                    is NavRoute.FlashcardStudy -> {
                        FlashcardStudyScreen(
                            uiState = flashcardUiState,
                            onEvent = { event ->
                                when (event) {
                                    is FlashcardEvent.FlipCard -> flashcardViewModel.flipCard()
                                    is FlashcardEvent.Grade -> flashcardViewModel.gradeCard(event.grade)
                                    is FlashcardEvent.RestartSession -> flashcardViewModel.restartSession()
                                    is FlashcardEvent.ExitSession -> {
                                        homeViewModel.loadHomeData()
                                        navManager.handleBack()
                                    }
                                }
                            }
                        )
                    }

                    is NavRoute.MistakesReview -> {
                        MistakesReviewScreen(
                            mistakes = openMistakes,
                            onMarkReviewed = { quizId, questionId ->
                                coroutineScope.launch {
                                    appContainer.mistakeRepository.markReviewed(quizId, questionId)
                                }
                            },
                            onClearAll = {
                                coroutineScope.launch {
                                    appContainer.mistakeRepository.clearAll()
                                }
                            },
                            onBack = { navManager.handleBack() }
                        )
                    }
                }
            }
        }
    }
}
