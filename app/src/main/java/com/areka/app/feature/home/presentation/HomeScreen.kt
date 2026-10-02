package com.areka.app.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.designsystem.*
import com.areka.app.data.model.SubjectItem

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    questionBank: QuestionBank,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        LoadingState(modifier = modifier.fillMaxSize(), message = "Loading study schedule...")
        return
    }

    if (uiState.error != null) {
        ErrorState(
            message = uiState.error,
            onRetry = { onEvent(HomeEvent.Refresh) },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header
        item(key = "header") {
            StudyHeader(
                greeting = uiState.greeting,
                name = uiState.profile.name,
                grade = uiState.profile.grade,
                streakDays = uiState.profile.streakDays,
                points = uiState.profile.totalPoints
            )
        }

        // 2. CONTINUE STUDYING (Primary Hero Card)
        item(key = "continue_studying") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                SectionHeader(
                    title = "Continue Studying",
                    subtitle = "Pick up where you left off"
                )
                Spacer(modifier = Modifier.height(10.dp))
                uiState.continueItem?.let { item ->
                    ContinueStudyHeroCard(
                        item = item,
                        onClick = {
                            when {
                                item.isMistake -> onEvent(HomeEvent.OpenMistakes(item.unitId))
                                item.isFlashcard -> onEvent(HomeEvent.OpenFlashcards(item.unitId, item.subjectId))
                                else -> onEvent(HomeEvent.OpenUnitQuiz(item.unitId, item.subjectId))
                            }
                        }
                    )
                }
            }
        }

        // 3. TODAY'S PLAN
        if (uiState.todayPlan.isNotEmpty()) {
            item(key = "today_plan_header") {
                SectionHeader(
                    title = "Today's Plan",
                    subtitle = "${uiState.todayPlan.size} key milestones to keep pace",
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }

            items(uiState.todayPlan, key = { "plan_${it.stepNumber}_${it.unitId}" }) { planItem ->
                TodayPlanCard(
                    item = planItem,
                    onClick = {
                        when (planItem.actionType) {
                            PlanActionType.REVIEW_MISTAKES -> onEvent(HomeEvent.OpenMistakes(planItem.unitId))
                            PlanActionType.STUDY_FLASHCARDS -> onEvent(HomeEvent.OpenFlashcards(planItem.unitId, planItem.subjectId))
                            PlanActionType.PRACTICE_QUIZ -> onEvent(HomeEvent.OpenUnitQuiz(planItem.unitId, planItem.subjectId))
                        }
                    },
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }

        // 4. QUICK ACTIONS
        item(key = "quick_actions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                SectionHeader(title = "Quick Actions")
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Practice",
                        subtitle = "By unit",
                        icon = Icons.Default.FitnessCenter,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onEvent(HomeEvent.QuickPractice) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_practice"
                    )
                    QuickActionButton(
                        title = "Flashcards",
                        subtitle = if (uiState.dueFlashcards > 0) "${uiState.dueFlashcards} due" else "Review",
                        icon = Icons.Outlined.Layers,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { onEvent(HomeEvent.QuickFlashcards) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_flashcards"
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Mistakes",
                        subtitle = if (uiState.openMistakes > 0) "${uiState.openMistakes} open" else "Clear",
                        icon = Icons.Outlined.ErrorOutline,
                        accentColor = if (uiState.openMistakes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        onClick = { onEvent(HomeEvent.OpenMistakes(null)) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_mistakes"
                    )
                    QuickActionButton(
                        title = "Progress",
                        subtitle = "Analytics",
                        icon = Icons.Outlined.Insights,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { onEvent(HomeEvent.QuickProgress) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_progress"
                    )
                }
            }
        }

        // 5. SUBJECTS
        item(key = "subjects_header") {
            SectionHeader(
                title = "Grade 10 Subjects",
                subtitle = "9 Curriculum Disciplines • 66 Units",
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        items(uiState.subjects, key = { "subject_${it.subjectId}" }) { progress ->
            val subjectItem = questionBank.getSubject(progress.subjectId) ?: SubjectItem(
                id = progress.subjectId,
                name = progress.subjectName,
                iconType = progress.iconType,
                quizCount = progress.totalUnits,
                accentColorHex = progress.accentColorHex
            )

            SubjectCard(
                subject = subjectItem,
                completedUnits = progress.completedUnits,
                totalUnits = progress.totalUnits,
                averageScore = progress.averageScorePercent,
                onClick = { onEvent(HomeEvent.OpenSubject(progress.subjectId)) },
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun StudyHeader(
    greeting: String,
    name: String,
    grade: String,
    streakDays: Int,
    points: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting, ${name.ifBlank { "Student" }}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$grade • Ministry of Education Curriculum",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(
                        label = "streak",
                        value = "${streakDays}d",
                        icon = Icons.Default.LocalFireDepartment,
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueStudyHeroCard(
    item: ContinueStudyItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when {
        item.isMistake -> MaterialTheme.colorScheme.error
        item.isFlashcard -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    val actionBadgeText = when {
        item.isMistake -> "Mistake review"
        item.isFlashcard -> "Spaced flashcard"
        else -> "Recommended next"
    }

    val actionButtonText = when {
        item.isMistake -> "Review"
        item.isFlashcard -> "Study"
        else -> "Continue"
    }

    ArekaV2Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = MaterialTheme.colorScheme.subtleBorder,
        elevation = 2.dp,
        testTag = "continue_study_hero_card",
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isMistake -> Icons.Default.Warning
                        item.isFlashcard -> Icons.Outlined.Layers
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actionBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = actionButtonText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TodayPlanCard(
    item: TodayPlanItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArekaV2Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        testTag = "today_plan_step_${item.stepNumber}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.stepNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }

            TextButton(onClick = onClick) {
                Text(
                    text = item.actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    ArekaV2Card(
        modifier = modifier,
        onClick = onClick,
        testTag = testTag,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }
    }
}
