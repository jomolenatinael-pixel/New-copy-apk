package com.areka.app.feature.quiz.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.areka.app.core.designsystem.*
import com.areka.app.data.model.QuestionType
import com.areka.app.data.model.QuizScoring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizActiveScreen(
    uiState: QuizUiState,
    onSelectOption: (String) -> Unit,
    onUpdateFillBlank: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quiz = uiState.quiz ?: return
    val currentQuestion = quiz.questions.getOrNull(uiState.currentQuestionIndex) ?: return
    val total = quiz.questions.size
    val currentNumber = uiState.currentQuestionIndex + 1
    val progressFraction = currentNumber.toFloat() / total.coerceAtLeast(1)

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Quiz?") },
            text = { Text("Your quiz progress will not be saved if you exit now.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onExitRequest()
                    }
                ) {
                    Text("Exit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Keep Going")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Question $currentNumber of $total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { showExitDialog = true },
                            modifier = Modifier.testTag("quiz_exit_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Quiz"
                            )
                        }
                    },
                    actions = {
                        val minutes = uiState.timeSpentSeconds / 60
                        val seconds = uiState.timeSpentSeconds % 60
                        val timeStr = "%02d:%02d".format(minutes, seconds)

                        StatChip(
                            label = "",
                            value = timeStr,
                            icon = Icons.Default.Timer,
                            containerColor = MaterialTheme.colorScheme.surfaceSubtle,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .testTag("quiz_progress_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceSubtle
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.subtleBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    if (!uiState.isSubmittedForCurrent) {
                        val canSubmit = if (currentQuestion.type == QuestionType.FILL_IN_THE_BLANK) {
                            uiState.fillBlankInput.trim().isNotBlank()
                        } else {
                            uiState.selectedOptionForCurrent != null
                        }

                        Button(
                            onClick = onSubmitAnswer,
                            enabled = canSubmit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("quiz_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Submit Answer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onNextQuestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("quiz_continue_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (currentNumber == total) "View Results" else "Continue",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
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
                .testTag("quiz_active_content"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Question Card
            item(key = "question_text") {
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "QUESTION $currentNumber",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentQuestion.text,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Answer Options or Fill in Blank
            if (currentQuestion.type == QuestionType.FILL_IN_THE_BLANK) {
                item(key = "fill_blank_input") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Type your answer:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = uiState.fillBlankInput,
                            onValueChange = onUpdateFillBlank,
                            enabled = !uiState.isSubmittedForCurrent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_fill_blank_input"),
                            shape = RoundedCornerShape(14.dp),
                            placeholder = { Text("Enter response...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            singleLine = true
                        )
                    }
                }
            } else {
                items(currentQuestion.options, key = { "opt_${it.id}" }) { option ->
                    val isSelected = uiState.selectedOptionForCurrent == option.id
                    val isCorrectState: Boolean? = if (uiState.isSubmittedForCurrent) {
                        if (option.id == currentQuestion.correctOptionId) true
                        else if (isSelected) false
                        else null
                    } else null

                    QuizOption(
                        letter = option.id,
                        text = option.text,
                        isSelected = isSelected,
                        isCorrectState = isCorrectState,
                        enabled = !uiState.isSubmittedForCurrent,
                        onClick = { onSelectOption(option.id) }
                    )
                }
            }

            // Feedback Card (revealed after submitting answer)
            if (uiState.isSubmittedForCurrent) {
                item(key = "feedback_panel") {
                    val submitted = if (currentQuestion.type == QuestionType.FILL_IN_THE_BLANK) {
                        uiState.fillBlankInput
                    } else {
                        uiState.selectedOptionForCurrent
                    }
                    val isCorrect = QuizScoring.isCorrect(currentQuestion, submitted)

                    ArekaV2Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (isCorrect) MaterialTheme.colorScheme.successContainer
                        else MaterialTheme.colorScheme.errorContainer,
                        borderColor = MaterialTheme.colorScheme.subtleBorder,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isCorrect) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isCorrect) "Correct answer" else "Incorrect answer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
