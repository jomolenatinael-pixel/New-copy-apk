package com.areka.app.feature.learn.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.designsystem.*
import com.areka.app.data.model.SubjectItem

@Composable
fun LearnScreen(
    uiState: LearnUiState,
    questionBank: QuestionBank,
    onEvent: (LearnEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.allSubjects.isEmpty()) {
        LoadingState(modifier = modifier.fillMaxSize(), message = "Loading Grade 10 subjects...")
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("learn_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "learn_header") {
            Column {
                Text(
                    text = "Learn Curriculum",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore all 9 Grade 10 subjects and 66 curriculum units",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        item(key = "search_bar") {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(LearnEvent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_subjects_input"),
                placeholder = {
                    Text(
                        text = "Search subjects or topics...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.textMuted
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onEvent(LearnEvent.SearchQueryChanged("")) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.textMuted
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.subtleBorder
                ),
                singleLine = true
            )
        }

        if (uiState.filteredSubjects.isEmpty() && !uiState.isLoading) {
            item(key = "empty_search") {
                EmptyState(
                    title = "No subjects found",
                    message = "No subjects matched \"${uiState.searchQuery}\"",
                    actionText = "Clear Search",
                    onActionClick = { onEvent(LearnEvent.SearchQueryChanged("")) }
                )
            }
        } else {
            items(uiState.filteredSubjects, key = { "learn_subj_${it.subjectId}" }) { progress ->
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
                    onClick = { onEvent(LearnEvent.SelectSubject(progress.subjectId)) }
                )
            }
        }
    }
}
