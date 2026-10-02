package com.areka.app.feature.progress.presentation

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
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*
import com.areka.app.data.model.BadgeType

@Composable
fun ProgressScreen(
    uiState: ProgressUiState,
    onEvent: (ProgressEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.subjectProgressList.isEmpty()) {
        LoadingState(modifier = modifier.fillMaxSize(), message = "Analyzing study statistics...")
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("progress_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "progress_header") {
            Column {
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Honest, verifiable curriculum analytics and achievements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // 1. Overall Progress Hero
        item(key = "overall_stats_grid") {
            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp)
            ) {
                Text(
                    text = "OVERALL PERFORMANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricColumn("Quizzes", "${uiState.overall.completedQuizzes}", Icons.Default.Quiz)
                    MetricColumn("Avg Score", "${uiState.overall.averageScorePercent}%", Icons.Default.CheckCircle)
                    MetricColumn("Study Time", "${uiState.overall.totalTimeStudiedHours}h", Icons.Default.Schedule)
                    MetricColumn("Streak", "${uiState.overall.streakDays}d", Icons.Default.LocalFireDepartment)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.subtleBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${uiState.overall.totalPoints} Total XP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.overall.openMistakesCount > 0) {
                        TextButton(
                            onClick = { onEvent(ProgressEvent.OpenMistakes) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = "${uiState.overall.openMistakesCount} open mistakes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "0 mistakes open",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.success
                        )
                    }
                }
            }
        }

        // 2. Trend / History Notice
        item(key = "trend_section") {
            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                SectionHeader(
                    title = "Activity History",
                    subtitle = "Session-by-session quiz accuracy"
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.overall.completedQuizzes < 3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceSubtle, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not enough activity yet (complete at least 3 quizzes to chart trend)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceSubtle, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Consistent study streak active. Current mastery pace: ${uiState.overall.completedQuizzes} completed attempts averaging ${uiState.overall.averageScorePercent}%.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Subject Progress Breakdown
        item(key = "subjects_progress_header") {
            SectionHeader(
                title = "Subject Mastery",
                subtitle = "Progress across Grade 10 subjects"
            )
        }

        items(uiState.subjectProgressList, key = { "prog_${it.subjectId}" }) { sp ->
            val accentColor = Color(sp.accentColorHex)
            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEvent(ProgressEvent.OpenSubject(sp.subjectId)) },
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
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getSubjectIcon(sp.iconType),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sp.subjectName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${sp.completedUnits} of ${sp.totalUnits} units • Avg ${sp.averageScorePercent}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                    }

                    ProgressRing(
                        progressPercent = sp.masteryPercent,
                        size = 42.dp,
                        strokeWidth = 4.dp,
                        primaryColor = accentColor
                    )
                }
            }
        }

        // 4. Streak Badges
        item(key = "streak_badges_header") {
            SectionHeader(
                title = "Daily Streak Milestones",
                subtitle = "Consecutive days of study"
            )
        }

        item(key = "streak_badges_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.streakBadges.forEach { badge ->
                    val color = Color(badge.colorHex)
                    ArekaV2Card(
                        modifier = Modifier.width(130.dp),
                        backgroundColor = if (badge.isUnlocked) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.subtleBorder,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (badge.isUnlocked) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (badge.isUnlocked) Icons.Default.EmojiEvents else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (badge.isUnlocked) color else MaterialTheme.colorScheme.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${badge.daysRequired} Days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.textMuted
                            )
                        }
                    }
                }
            }
        }

        // 5. Achievements
        item(key = "achievements_header") {
            SectionHeader(
                title = "Achievements",
                subtitle = "${uiState.achievements.count { it.unlocked }} of ${uiState.achievements.size} unlocked"
            )
        }

        items(uiState.achievements, key = { "ach_${it.id}" }) { ach ->
            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (ach.unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.subtleBorder,
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (ach.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceSubtle
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (ach.unlocked) Icons.Default.CheckCircle else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (ach.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ach.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ach.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                    }
                }
            }
        }

        // 6. Leaderboard
        item(key = "leaderboard_header") {
            SectionHeader(
                title = "Curriculum Leaderboard",
                subtitle = if (uiState.leaderboard.isEmpty()) "Sign in to join community rankings" else "Top learners in Grade 10"
            )
        }

        if (uiState.leaderboard.isEmpty()) {
            item(key = "empty_leaderboard") {
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Text(
                        text = "Community Leaderboard",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Leaderboard rankings are synced with the cloud for verified student accounts. Guest mode keeps study progress strictly offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }
            }
        } else {
            items(uiState.leaderboard.take(10), key = { "lb_${it.id}" }) { entry ->
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.subtleBorder,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "#${entry.rank}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (entry.badgeType) {
                                BadgeType.GOLD -> Color(0xFFEAB308)
                                BadgeType.SILVER -> Color(0xFF94A3B8)
                                BadgeType.BRONZE -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.textMuted
                            },
                            modifier = Modifier.width(36.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (entry.isCurrentUser) "${entry.name} (You)" else entry.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = entry.grade,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted
                            )
                        }

                        Text(
                            text = "${entry.points} pts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted
        )
    }
}
