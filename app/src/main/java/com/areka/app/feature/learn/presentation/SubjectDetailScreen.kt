package com.areka.app.feature.learn.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.areka.app.data.model.SubjectItem
import com.areka.app.data.model.SubjectProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: SubjectItem,
    progress: SubjectProgress?,
    units: List<UnitWithProgress>,
    onUnitClick: (String) -> Unit,
    onQuizClick: (String) -> Unit,
    onFlashcardsClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(subject.accentColorHex)
    val completedUnits = progress?.completedUnits ?: 0
    val totalUnits = units.size
    val progressPercent = if (totalUnits == 0) 0 else (completedUnits * 100 / totalUnits).coerceIn(0, 100)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subject_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("subject_detail_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject Hero Banner
            item(key = "subject_hero") {
                ArekaV2Card(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.subtleBorder,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getSubjectIcon(subject.iconType),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(30.dp)
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
                                text = subject.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted
                            )
                        }

                        ProgressRing(
                            progressPercent = progressPercent,
                            size = 54.dp,
                            strokeWidth = 5.dp,
                            primaryColor = accentColor
                        )
                    }
                }
            }

            item(key = "units_header") {
                SectionHeader(
                    title = "Curriculum Units",
                    subtitle = "$completedUnits of $totalUnits units mastered"
                )
            }

            items(units, key = { "unit_row_${it.unit.id}" }) { item ->
                val isCompleted = item.progress.quizAccuracyPercent >= 70
                UnitRow(
                    unit = item.unit,
                    accuracyPercent = item.progress.quizAccuracyPercent,
                    isCompleted = isCompleted,
                    onQuizClick = { onQuizClick(item.unit.id) },
                    onFlashcardsClick = { onFlashcardsClick(item.unit.id) },
                    onUnitClick = { onUnitClick(item.unit.id) }
                )
            }
        }
    }
}
