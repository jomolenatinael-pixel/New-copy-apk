package com.areka.app.feature.practice.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*
import com.areka.app.core.repository.ActionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    uiState: PracticeUiState,
    onEvent: (PracticeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.subjects.isEmpty()) {
        LoadingState(modifier = modifier.fillMaxSize(), message = "Loading practice hub...")
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("practice_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "practice_header") {
            Column {
                Text(
                    text = "Practice Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Targeted quizzes, weak area reinforcement, and past attempts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // Category Segmented Tabs
        item(key = "categories_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PracticeCategory.entries.forEach { category ->
                    val isSelected = uiState.selectedCategory == category
                    val label = when (category) {
                        PracticeCategory.RECOMMENDED -> "Recommended"
                        PracticeCategory.BY_SUBJECT -> "By Subject"
                        PracticeCategory.WEAK_AREAS -> "Weak Areas (${uiState.weakAreas.size})"
                        PracticeCategory.RECENT -> "Recent (${uiState.recentAttempts.size})"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEvent(PracticeEvent.SelectCategory(category)) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                    )
                }
            }
        }

        // Content based on selected Category
        when (uiState.selectedCategory) {
            PracticeCategory.RECOMMENDED -> {
                if (uiState.recommendations.isEmpty()) {
                    item(key = "empty_rec") {
                        EmptyState(
                            title = "All caught up!",
                            message = "No urgent recommendations right now. Select a subject below to keep practicing."
                        )
                    }
                } else {
                    items(uiState.recommendations, key = { "rec_${it.id}" }) { rec ->
                        StudyActionCard(
                            title = rec.title,
                            subtitle = rec.subtitle,
                            reason = rec.reason,
                            actionButtonText = when (rec.actionType) {
                                ActionType.PRACTICE_QUIZ -> "Start Quiz"
                                ActionType.REVIEW_FLASHCARDS -> "Study Cards"
                                ActionType.REVIEW_MISTAKES -> "Review Mistakes"
                            },
                            icon = when (rec.actionType) {
                                ActionType.PRACTICE_QUIZ -> Icons.Default.PlayArrow
                                ActionType.REVIEW_FLASHCARDS -> Icons.Outlined.Layers
                                ActionType.REVIEW_MISTAKES -> Icons.Default.Warning
                            },
                            accentColor = when (rec.actionType) {
                                ActionType.PRACTICE_QUIZ -> MaterialTheme.colorScheme.primary
                                ActionType.REVIEW_FLASHCARDS -> MaterialTheme.colorScheme.secondary
                                ActionType.REVIEW_MISTAKES -> MaterialTheme.colorScheme.error
                            },
                            onActionClick = {
                                when (rec.actionType) {
                                    ActionType.PRACTICE_QUIZ -> onEvent(PracticeEvent.StartQuiz("quiz_${rec.unitId}", rec.unitId, rec.subjectId))
                                    ActionType.REVIEW_FLASHCARDS -> onEvent(PracticeEvent.StudyFlashcards(rec.unitId, rec.subjectId))
                                    ActionType.REVIEW_MISTAKES -> onEvent(PracticeEvent.ReviewMistakes(rec.unitId))
                                }
                            }
                        )
                    }
                }
            }

            PracticeCategory.BY_SUBJECT -> {
                // Subject Filter Chips
                item(key = "subject_filter_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.subjects.forEach { subject ->
                            val isSelected = uiState.selectedSubjectId == subject.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onEvent(PracticeEvent.SelectSubjectFilter(subject.id)) },
                                label = { Text(subject.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getSubjectIcon(subject.iconType),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                items(uiState.subjectUnits, key = { "subj_unit_${it.id}" }) { unit ->
                    ArekaV2Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onEvent(PracticeEvent.StartQuiz("quiz_${unit.id}", unit.id, unit.subjectId))
                        },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "U${unit.unitNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unit ${unit.unitNumber}: ${unit.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = unit.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { onEvent(PracticeEvent.StartQuiz("quiz_${unit.id}", unit.id, unit.subjectId)) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = "Start", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            PracticeCategory.WEAK_AREAS -> {
                if (uiState.weakAreas.isEmpty()) {
                    item(key = "empty_weak") {
                        EmptyState(
                            title = "No weak areas detected",
                            message = "Great job! All your attempted quizzes have high scores and no open mistakes."
                        )
                    }
                } else {
                    items(uiState.weakAreas, key = { "weak_${it.unitId}" }) { weak ->
                        ArekaV2Card(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = MaterialTheme.colorScheme.subtleBorder,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${weak.subjectName} • ${weak.unitTitle}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Accuracy: ${weak.accuracyPercent}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        if (weak.mistakeCount > 0) {
                                            Text(
                                                text = "• ${weak.mistakeCount} mistakes",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.textMuted
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onEvent(PracticeEvent.StartQuiz("quiz_${weak.unitId}", weak.unitId, weak.subjectId)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "Practice", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            PracticeCategory.RECENT -> {
                if (uiState.recentAttempts.isEmpty()) {
                    item(key = "empty_recent") {
                        EmptyState(
                            title = "No recent attempts yet",
                            message = "Complete quizzes to see your history and scores logged here."
                        )
                    }
                } else {
                    items(uiState.recentAttempts, key = { "recent_${it.id}" }) { attempt ->
                        val dateFormatted = try {
                            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                            sdf.format(Date(attempt.completedAtEpochMillis))
                        } catch (_: Exception) {
                            "Recently completed"
                        }

                        ArekaV2Card(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (attempt.scorePercent >= 70) MaterialTheme.colorScheme.successContainer
                                            else MaterialTheme.colorScheme.errorContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${attempt.scorePercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (attempt.scorePercent >= 70) MaterialTheme.colorScheme.success
                                        else MaterialTheme.colorScheme.error
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attempt.quizTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${attempt.correctAnswers}/${attempt.totalQuestions} correct • $dateFormatted",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.textMuted
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        onEvent(PracticeEvent.StartQuiz(attempt.quizId, attempt.unitId, attempt.subjectId))
                                    }
                                ) {
                                    Text(text = "Retake", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
