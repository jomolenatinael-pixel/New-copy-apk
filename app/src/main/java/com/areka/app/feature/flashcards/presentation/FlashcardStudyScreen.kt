package com.areka.app.feature.flashcards.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*
import com.areka.app.data.local.ReviewGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(
    uiState: FlashcardUiState,
    onEvent: (FlashcardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        LoadingState(modifier = modifier.fillMaxSize(), message = "Loading flashcard deck...")
        return
    }

    if (uiState.cards.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Flashcards") },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(FlashcardEvent.ExitSession) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            EmptyState(
                title = "No Flashcards",
                message = "This unit currently has no flashcards authored.",
                actionText = "Go Back",
                onActionClick = { onEvent(FlashcardEvent.ExitSession) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
        return
    }

    val total = uiState.cards.size
    val currentNumber = (uiState.currentIndex + 1).coerceAtMost(total)
    val remaining = (total - currentNumber).coerceAtLeast(0)
    val progressFraction = currentNumber.toFloat() / total.coerceAtLeast(1)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.isSessionFinished) "Deck Completed" else "Card $currentNumber of $total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { onEvent(FlashcardEvent.ExitSession) },
                            modifier = Modifier.testTag("flashcard_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (!uiState.isSessionFinished) {
                            StatChip(
                                label = "remaining",
                                value = "$remaining",
                                containerColor = MaterialTheme.colorScheme.surfaceSubtle,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .testTag("flashcard_progress_bar"),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceSubtle
                )
            }
        },
        bottomBar = {
            if (!uiState.isSessionFinished) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.subtleBorder)
                ) {
                    if (!uiState.isFlipped) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Button(
                                onClick = { onEvent(FlashcardEvent.FlipCard) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("flashcard_reveal_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = "Reveal Answer",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // SM2 Grade Buttons: Again, Hard, Good, Easy with real interval preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GradeButton(
                                label = "Again",
                                interval = uiState.againInterval,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error,
                                onClick = { onEvent(FlashcardEvent.Grade(ReviewGrade.AGAIN)) },
                                modifier = Modifier.weight(1f),
                                testTag = "grade_again"
                            )
                            GradeButton(
                                label = "Hard",
                                interval = uiState.hardInterval,
                                containerColor = MaterialTheme.colorScheme.surfaceSubtle,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                onClick = { onEvent(FlashcardEvent.Grade(ReviewGrade.HARD)) },
                                modifier = Modifier.weight(1f),
                                testTag = "grade_hard"
                            )
                            GradeButton(
                                label = "Good",
                                interval = uiState.goodInterval,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                                onClick = { onEvent(FlashcardEvent.Grade(ReviewGrade.GOOD)) },
                                modifier = Modifier.weight(1f),
                                testTag = "grade_good"
                            )
                            GradeButton(
                                label = "Easy",
                                interval = uiState.easyInterval,
                                containerColor = MaterialTheme.colorScheme.successContainer,
                                contentColor = MaterialTheme.colorScheme.success,
                                onClick = { onEvent(FlashcardEvent.Grade(ReviewGrade.EASY)) },
                                modifier = Modifier.weight(1f),
                                testTag = "grade_easy"
                            )
                        }
                    }
                }
            } else {
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
                            onClick = { onEvent(FlashcardEvent.RestartSession) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("flashcard_restart_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Study Again")
                        }
                        Button(
                            onClick = { onEvent(FlashcardEvent.ExitSession) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("flashcard_finish_button"),
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
        if (!uiState.isSessionFinished) {
            val card = uiState.cards[uiState.currentIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                FlashcardComponent(
                    frontText = card.front,
                    backText = card.back,
                    isFlipped = uiState.isFlipped,
                    onFlip = { onEvent(FlashcardEvent.FlipCard) },
                    subjectName = uiState.subjectName,
                    unitTitle = uiState.unitTitle
                )
            }
        } else {
            // End of Session Summary
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("flashcard_summary_screen"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "summary_card") {
                    ArekaV2Card(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = MaterialTheme.colorScheme.subtleBorder,
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Session Complete!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.subjectName} • ${uiState.unitTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.textMuted
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${uiState.reviewedCount}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Reviewed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.textMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${uiState.masteredCount}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.success
                                    )
                                    Text(
                                        text = "Mastered",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.textMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "0",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.textMuted
                                    )
                                    Text(
                                        text = "Remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    interval: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = interval,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
