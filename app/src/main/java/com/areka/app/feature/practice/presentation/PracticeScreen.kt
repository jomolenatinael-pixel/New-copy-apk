package com.areka.app.feature.practice.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Layers
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
import com.areka.app.data.model.BadgeType
import com.areka.app.data.model.LeaderboardEntry
import com.areka.app.data.model.SubjectItem
import com.areka.app.data.model.SubjectUnit
import java.text.NumberFormat
import java.util.Locale

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("practice_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A) Header
        item(key = "practice_header") {
            Column(modifier = Modifier.padding(bottom = 2.dp)) {
                Text(
                    text = "Practice",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pick a subject to practice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // B) Beautiful SUBJECTS section (main content)
        items(uiState.subjects, key = { "subject_${it.id}" }) { subject ->
            val isExpanded = uiState.expandedSubjectId == subject.id
            val units = uiState.subjectUnitsMap[subject.id].orEmpty()
            val completedUnits = uiState.subjectCompletedUnitsMap[subject.id] ?: 0
            val accentColor = Color(subject.accentColorHex)

            PracticeSubjectCard(
                subject = subject,
                units = units,
                completedUnits = completedUnits,
                isExpanded = isExpanded,
                accentColor = accentColor,
                onToggleExpand = { onEvent(PracticeEvent.ToggleSubject(subject.id)) },
                onStartQuiz = { unitId ->
                    onEvent(PracticeEvent.StartQuiz("quiz_$unitId", unitId, subject.id))
                },
                onStudyFlashcards = { unitId ->
                    onEvent(PracticeEvent.StudyFlashcards(unitId, subject.id))
                },
                onOpenSubject = {
                    onEvent(PracticeEvent.OpenSubject(subject.id))
                }
            )
        }

        // C) LEADERBOARD section on Practice
        item(key = "leaderboard_divider") {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.subtleBorder)
        }

        item(key = "leaderboard_header") {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Top Grade 10 students",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        if (uiState.leaderboard.isEmpty()) {
            item(key = "leaderboard_empty") {
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = "Rankings will appear as students complete quizzes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }
            }
        } else {
            items(uiState.leaderboard.take(10), key = { "lb_${it.id}_${it.rank}" }) { entry ->
                LeaderboardCompactRow(
                    entry = entry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PracticeSubjectCard(
    subject: SubjectItem,
    units: List<SubjectUnit>,
    completedUnits: Int,
    isExpanded: Boolean,
    accentColor: Color,
    onToggleExpand: () -> Unit,
    onStartQuiz: (unitId: String) -> Unit,
    onStudyFlashcards: (unitId: String) -> Unit,
    onOpenSubject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUnits = if (units.isNotEmpty()) units.size else subject.quizCount

    ArekaV2Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("practice_subject_${subject.id}"),
        onClick = onToggleExpand,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getSubjectIcon(subject.iconType),
                        contentDescription = subject.name,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (completedUnits > 0) "$totalUnits units • $completedUnits completed" else "$totalUnits units",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.testTag("toggle_subject_${subject.id}")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse units" else "Expand units",
                        tint = MaterialTheme.colorScheme.textMuted
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.subtleBorder)

                    if (units.isEmpty()) {
                        Text(
                            text = "Loading units...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        units.forEach { unit ->
                            PracticeUnitRow(
                                unit = unit,
                                onStartQuiz = { onStartQuiz(unit.id) },
                                onStudyFlashcards = { onStudyFlashcards(unit.id) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onOpenSubject,
                            modifier = Modifier.testTag("open_subject_${subject.id}")
                        ) {
                            Text(
                                text = "View Subject Overview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeUnitRow(
    unit: SubjectUnit,
    onStartQuiz: () -> Unit,
    onStudyFlashcards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceSubtle,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U${unit.unitNumber}",
                        style = MaterialTheme.typography.labelSmall,
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
                    if (unit.description.isNotBlank()) {
                        Text(
                            text = unit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("unit_quiz_btn_${unit.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Take Quiz",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onStudyFlashcards,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("unit_flashcard_btn_${unit.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Flashcards",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardCompactRow(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier
) {
    val rankColor = when (entry.badgeType) {
        BadgeType.GOLD -> Color(0xFFEAB308)
        BadgeType.SILVER -> Color(0xFF94A3B8)
        BadgeType.BRONZE -> Color(0xFFCD7F32)
        BadgeType.REGULAR -> MaterialTheme.colorScheme.textMuted
    }

    val isCurrent = entry.isCurrentUser
    val formattedPoints = try {
        NumberFormat.getNumberInstance(Locale.US).format(entry.points)
    } catch (_: Exception) {
        "${entry.points}"
    }

    ArekaV2Card(
        modifier = modifier.testTag("leaderboard_row_${entry.rank}"),
        backgroundColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surface,
        borderColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.subtleBorder,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = rankColor,
                modifier = Modifier.width(32.dp)
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(entry.avatarColorHex).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (entry.name.firstOrNull() ?: 'S').uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(entry.avatarColorHex)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = entry.grade,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }

            Text(
                text = "$formattedPoints pts",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
