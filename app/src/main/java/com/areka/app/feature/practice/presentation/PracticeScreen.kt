package com.areka.app.feature.practice.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*

@Composable
fun PracticeScreen(
    uiState: PracticeUiState,
    onEvent: (PracticeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.subjects.isEmpty()) {
        LoadingState(modifier = modifier.fillMaxSize(), message = "Loading subjects...")
        return
    }

    if (uiState.error != null && uiState.subjects.isEmpty()) {
        ErrorState(
            modifier = modifier.fillMaxSize(),
            message = uiState.error,
            onRetry = { onEvent(PracticeEvent.Refresh) }
        )
        return
    }

    if (!uiState.isLoading && uiState.subjects.isEmpty()) {
        EmptyState(
            modifier = modifier.fillMaxSize(),
            title = "No subjects found",
            message = "Curriculum subjects will appear here.",
            actionText = "Reload",
            onActionClick = { onEvent(PracticeEvent.Refresh) }
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("practice_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // A) Header
        item(span = { GridItemSpan(maxLineSpan) }, key = "practice_header") {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "Practice",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose a subject to start practicing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // B) Responsive 2-Column Subject Grid (Main Content)
        items(uiState.subjects, key = { "subject_${it.id}" }) { subject ->
            val unitsCount = uiState.subjectUnitsCountMap[subject.id] ?: subject.quizCount
            val quizzesCount = uiState.subjectQuizzesCountMap[subject.id] ?: subject.quizCount

            ArekaSubjectGridCard(
                subject = subject,
                unitsCount = unitsCount,
                quizzesCount = quizzesCount,
                onClick = { onEvent(PracticeEvent.OpenSubject(subject.id)) },
                testTag = "practice_subject_${subject.id}"
            )
        }

        // C) Divider
        item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_divider") {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.subtleBorder)
            Spacer(modifier = Modifier.height(6.dp))
        }

        // D) Leaderboard Section Header
        item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Top Grade 10 students",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }

                TextButton(
                    onClick = { onEvent(PracticeEvent.ViewFullLeaderboard) },
                    modifier = Modifier.testTag("leaderboard_view_all_button")
                ) {
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View full leaderboard",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // E) Leaderboard Rows (Compact, Top 10)
        if (uiState.leaderboard.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_empty") {
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Leaderboard isn't available right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { onEvent(PracticeEvent.Refresh) }) {
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            items(
                uiState.leaderboard.take(10),
                span = { GridItemSpan(maxLineSpan) },
                key = { "lb_${it.id}_${it.rank}" }
            ) { entry ->
                ArekaCompactLeaderboardRow(
                    entry = entry,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "leaderboard_row_${entry.rank}"
                )
            }
        }
    }
}
