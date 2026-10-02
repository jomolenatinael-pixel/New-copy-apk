package com.areka.app.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*
import com.areka.app.data.local.MistakeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesReviewScreen(
    mistakes: List<MistakeEntity>,
    onMarkReviewed: (String, Int) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mistakes Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("mistakes_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (mistakes.isNotEmpty()) {
                        IconButton(onClick = onClearAll, modifier = Modifier.testTag("clear_mistakes_button")) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All Mistakes",
                                tint = MaterialTheme.colorScheme.textMuted
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (mistakes.isEmpty()) {
            EmptyState(
                title = "Zero Open Mistakes",
                message = "You have no unreviewed mistakes! You understand all questions you've attempted so far.",
                actionText = "Back to Practice",
                onActionClick = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("mistakes_list_screen"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "mistakes_count_banner") {
                    Text(
                        text = "${mistakes.size} questions need review",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                items(mistakes, key = { "${it.quizId}_${it.questionId}" }) { mistake ->
                    MistakeCard(
                        questionText = mistake.questionText,
                        wrongAnswer = mistake.selectedAnswer,
                        correctAnswer = mistake.correctAnswer,
                        explanation = mistake.explanation,
                        onMarkReviewed = { onMarkReviewed(mistake.quizId, mistake.questionId) }
                    )
                }
            }
        }
    }
}
