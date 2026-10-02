package com.areka.app.feature.learn.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*
import com.areka.app.data.model.Flashcard
import com.areka.app.data.model.SubjectUnit
import com.areka.app.data.model.UnitProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDetailScreen(
    unit: SubjectUnit,
    progress: UnitProgress?,
    flashcards: List<Flashcard>,
    onStartQuiz: () -> Unit,
    onStudyFlashcards: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Read / Review, 1: Flashcards overview

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Unit ${unit.unitNumber}: ${unit.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("unit_back_button")) {
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onStudyFlashcards,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("unit_action_flashcards"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Flashcards (${flashcards.size})")
                    }

                    Button(
                        onClick = onStartQuiz,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("unit_action_quiz"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Practice Quiz")
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
                .testTag("unit_detail_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Unit Overview Card
            item(key = "unit_overview") {
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Text(
                        text = "CURRICULUM OBJECTIVES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = unit.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatChip(
                            label = "attempts",
                            value = "${progress?.quizAttempts ?: 0}",
                            containerColor = MaterialTheme.colorScheme.surfaceSubtle
                        )
                        StatChip(
                            label = "accuracy",
                            value = "${progress?.quizAccuracyPercent ?: 0}%",
                            containerColor = MaterialTheme.colorScheme.surfaceSubtle
                        )
                        StatChip(
                            label = "mastery",
                            value = "${progress?.masteryPercent ?: 0}%",
                            containerColor = MaterialTheme.colorScheme.surfaceSubtle
                        )
                    }
                }
            }

            // READ / REVIEW Section: Key principles and definitions from real curriculum cards
            item(key = "review_header") {
                SectionHeader(
                    title = "Key Concepts & Principles",
                    subtitle = "Foundational definitions for Unit ${unit.unitNumber}"
                )
            }

            if (flashcards.isEmpty()) {
                item(key = "empty_concepts") {
                    EmptyState(
                        title = "No concepts authored",
                        message = "Start the unit quiz directly below."
                    )
                }
            } else {
                items(flashcards, key = { "concept_${it.id}" }) { card ->
                    ArekaV2Card(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = card.front,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = card.back,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
