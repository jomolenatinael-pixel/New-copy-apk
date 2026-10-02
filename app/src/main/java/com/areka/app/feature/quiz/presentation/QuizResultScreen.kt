package com.areka.app.feature.quiz.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*
import com.areka.app.data.model.Quiz

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    uiState: QuizUiState,
    onRetake: () -> Unit,
    onReviewMistakes: () -> Unit,
    onBackToSubject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quiz = uiState.quiz ?: return
    val score = uiState.score ?: return
    val minutes = uiState.timeSpentSeconds / 60
    val seconds = uiState.timeSpentSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    val passed = score.percentage >= 70

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quiz Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToSubject) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.subtleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("result_retake_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Again")
                    }

                    if (uiState.mistakes.isNotEmpty()) {
                        Button(
                            onClick = onReviewMistakes,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("result_review_mistakes_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mistakes (${uiState.mistakes.size})")
                        }
                    } else {
                        Button(
                            onClick = onBackToSubject,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("result_back_subject_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("quiz_result_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Score Hero Card
            item(key = "score_hero") {
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.subtleBorder,
                    contentPadding = PaddingValues(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProgressRing(
                            progressPercent = score.percentage,
                            size = 80.dp,
                            strokeWidth = 8.dp,
                            primaryColor = if (passed) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (passed) "Quiz passed" else "Review needed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = quiz.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${score.correctAnswers}/${score.totalQuestions}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Correct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+${score.pointsEarned}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Points",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // What You Mastered
            if (uiState.masteredQuestions.isNotEmpty()) {
                item(key = "mastered_header") {
                    SectionHeader(
                        title = "What You Mastered",
                        subtitle = "${uiState.masteredQuestions.size} questions answered correctly"
                    )
                }

                items(uiState.masteredQuestions, key = { "mastered_${it.id}" }) { q ->
                    ArekaV2Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.subtleBorder,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.success,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = q.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // What Needs Review
            if (uiState.reviewNeededQuestions.isNotEmpty()) {
                item(key = "review_header") {
                    SectionHeader(
                        title = "Needs Review",
                        subtitle = "${uiState.reviewNeededQuestions.size} questions missed"
                    )
                }

                items(uiState.reviewNeededQuestions, key = { "review_${it.id}" }) { q ->
                    val userAns = uiState.userAnswers[q.id] ?: "Skipped"
                    val mistake = uiState.mistakes.find { it.questionId == q.id }
                    MistakeCard(
                        questionText = q.text,
                        wrongAnswer = mistake?.selectedAnswer ?: userAns,
                        correctAnswer = mistake?.correctAnswer ?: q.correctOptionId,
                        explanation = q.explanation,
                        onMarkReviewed = { /* logged in Room already */ }
                    )
                }
            }
        }
    }
}
