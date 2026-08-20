package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.FlashcardItem
import com.example.core.ai.QuizData
import com.example.core.ai.RevisionSummary
import com.example.ui.theme.MummaEmerald
import com.example.ui.theme.MummaPrimaryAmber
import com.example.ui.theme.MummaRose
import com.example.ui.theme.MummaSecondaryCyan
import com.example.ui.theme.MummaTertiaryViolet
import com.example.ui.viewmodel.FlashcardsUiState
import com.example.ui.viewmodel.MummaViewModel
import com.example.ui.viewmodel.QuizUiState
import com.example.ui.viewmodel.RevisionUiState

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ViewInAr
import com.example.ui.components.MultimodalExplanationCard
import com.example.ui.viewmodel.TopicExplanationUiState

enum class StudySubView {
    MENU,
    EXPLANATION,
    QUIZ,
    FLASHCARDS,
    REVISION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModeScreen(
    viewModel: MummaViewModel,
    onBack: () -> Unit,
    onNavigateToTeachMumma: (String) -> Unit
) {
    val selectedTopic by viewModel.selectedStudyTopic.collectAsState()
    val topicExplanationState by viewModel.topicExplanationState.collectAsState()
    val quizState by viewModel.quizState.collectAsState()
    val flashcardState by viewModel.flashcardState.collectAsState()
    val revisionState by viewModel.revisionState.collectAsState()

    var currentView by remember { mutableStateOf(StudySubView.MENU) }
    var topicInput by remember { mutableStateOf(selectedTopic) }

    val popularTopics = listOf("Photosynthesis", "Newton's Laws", "Cell Mitosis", "Organic Chemistry", "Quantum Mechanics")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MummaSecondaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (currentView == StudySubView.MENU) "Study Mode" else selectedTopic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentView != StudySubView.MENU) {
                                currentView = StudySubView.MENU
                                viewModel.resetTopicExplanation()
                                viewModel.resetQuiz()
                                viewModel.resetFlashcards()
                                viewModel.resetRevision()
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("study_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentView) {
                StudySubView.MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Topic Input Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Target Study Topic",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = topicInput,
                                    onValueChange = {
                                        topicInput = it
                                        viewModel.setSelectedStudyTopic(it)
                                    },
                                    placeholder = { Text("e.g. Photosynthesis, Cellular Respiration") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("study_topic_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MummaSecondaryCyan,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick topic chips
                                Text(
                                    text = "Quick Topics:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(popularTopics) { popTopic ->
                                        val isSelected = popTopic.equals(topicInput, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) MummaSecondaryCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MummaSecondaryCyan else MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(20.dp)
                                                )
                                                .clickable {
                                                    topicInput = popTopic
                                                    viewModel.setSelectedStudyTopic(popTopic)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = popTopic,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) MummaSecondaryCyan else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Study Modes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 1. MUMMA MULTIMODAL EXPLANATION (NEW CORE FEATURE)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    1.5.dp,
                                    Brush.horizontalGradient(listOf(MummaSecondaryCyan, MummaEmerald)),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    val targetTopic = topicInput.ifBlank { "Photosynthesis" }
                                    viewModel.setSelectedStudyTopic(targetTopic)
                                    viewModel.explainTopic(targetTopic)
                                    currentView = StudySubView.EXPLANATION
                                }
                                .testTag("explain_multimodal_mode_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MummaSecondaryCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewInAr,
                                        contentDescription = "Explain Topic",
                                        tint = Color(0xFF002B33),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Multimodal Explanation",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MummaSecondaryCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "3D & Flowcharts",
                                                color = MummaSecondaryCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Mumma explains using step flowcharts, kinetic 3D models, structural blueprints & audio in 3 languages.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 2. TEACH MUMMA FEATURE (HIGHLIGHTED)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    1.5.dp,
                                    Brush.horizontalGradient(listOf(MummaPrimaryAmber, MummaTertiaryViolet)),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    onNavigateToTeachMumma(topicInput.ifBlank { "Photosynthesis" })
                                }
                                .testTag("teach_mumma_mode_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MummaPrimaryAmber),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Teach Mumma",
                                        tint = Color(0xFF1E1402),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Teach Mumma",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MummaPrimaryAmber.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Feynman Method",
                                                color = MummaPrimaryAmber,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Explain the topic verbally. Mumma listens and provides understanding, missing concepts & corrections.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 3. QUIZ ME
                        StudyModeOptionCard(
                            icon = Icons.Default.Quiz,
                            iconColor = MummaSecondaryCyan,
                            title = "Quiz Me",
                            description = "Interactive test to evaluate conceptual mastery and memory.",
                            onClick = {
                                viewModel.startQuiz(topicInput.ifBlank { "Photosynthesis" })
                                currentView = StudySubView.QUIZ
                            },
                            tag = "quiz_me_option"
                        )

                        // 4. FLASHCARDS
                        StudyModeOptionCard(
                            icon = Icons.Default.Flip,
                            iconColor = MummaTertiaryViolet,
                            title = "Flashcards",
                            description = "Active recall drill with flipping key terms and definitions.",
                            onClick = {
                                viewModel.loadFlashcards(topicInput.ifBlank { "Photosynthesis" })
                                currentView = StudySubView.FLASHCARDS
                            },
                            tag = "flashcards_option"
                        )

                        // 5. QUICK REVISION
                        StudyModeOptionCard(
                            icon = Icons.Default.Lightbulb,
                            iconColor = MummaEmerald,
                            title = "Quick Revision",
                            description = "High-yield core definitions, key points, and common traps.",
                            onClick = {
                                viewModel.loadQuickRevision(topicInput.ifBlank { "Photosynthesis" })
                                currentView = StudySubView.REVISION
                            },
                            tag = "quick_revision_option"
                        )
                    }
                }

                StudySubView.EXPLANATION -> {
                    when (val state = topicExplanationState) {
                        is TopicExplanationUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MummaSecondaryCyan,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Mumma is preparing multimodal flowcharts & 3D models…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is TopicExplanationUiState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                MultimodalExplanationCard(
                                    explanation = state.explanation,
                                    onSpeak = { text -> viewModel.speakText(text) },
                                    onFollowUpAction = { action ->
                                        if (action.contains("quiz", ignoreCase = true)) {
                                            viewModel.startQuiz(state.explanation.topic)
                                            currentView = StudySubView.QUIZ
                                        } else if (action.contains("flashcard", ignoreCase = true)) {
                                            viewModel.loadFlashcards(state.explanation.topic)
                                            currentView = StudySubView.FLASHCARDS
                                        } else if (action.contains("teach", ignoreCase = true)) {
                                            onNavigateToTeachMumma(state.explanation.topic)
                                        } else if (action.contains("revision", ignoreCase = true) || action.contains("summary", ignoreCase = true)) {
                                            viewModel.loadQuickRevision(state.explanation.topic)
                                            currentView = StudySubView.REVISION
                                        } else {
                                            viewModel.explainTopic(state.explanation.topic)
                                        }
                                    }
                                )
                            }
                        }
                        is TopicExplanationUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MummaRose,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { viewModel.explainTopic(selectedTopic) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MummaSecondaryCyan)
                                    ) {
                                        Text("Retry Explanation")
                                    }
                                }
                            }
                        }
                        is TopicExplanationUiState.Idle -> {
                            currentView = StudySubView.MENU
                        }
                    }
                }

                StudySubView.QUIZ -> {
                    QuizView(
                        state = quizState,
                        onOptionSelected = viewModel::selectQuizOption,
                        onNext = viewModel::nextQuizQuestion,
                        onRestart = { viewModel.startQuiz(selectedTopic) },
                        onBackToMenu = { currentView = StudySubView.MENU }
                    )
                }

                StudySubView.FLASHCARDS -> {
                    FlashcardsView(
                        state = flashcardState,
                        onFlip = viewModel::flipCurrentFlashcard,
                        onNext = viewModel::nextFlashcard,
                        onPrevious = viewModel::previousFlashcard,
                        onBackToMenu = { currentView = StudySubView.MENU }
                    )
                }

                StudySubView.REVISION -> {
                    RevisionView(
                        state = revisionState,
                        onSpeakTip = { tip -> viewModel.speakText(tip) },
                        onBackToMenu = { currentView = StudySubView.MENU }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyModeOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuizView(
    state: QuizUiState,
    onOptionSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit
) {
    when (state) {
        is QuizUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MummaSecondaryCyan)
            }
        }
        is QuizUiState.InProgress -> {
            val q = state.data.questions[state.currentIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Question ${state.currentIndex + 1} of ${state.data.questions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MummaSecondaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score: ${state.score}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Option Buttons
                    q.options.forEachIndexed { index, option ->
                        val isSelected = state.selectedOption == index
                        val isCorrect = index == q.correctIndex
                        val btnColor = when {
                            !state.isAnswered -> MaterialTheme.colorScheme.surfaceVariant
                            isCorrect -> MummaEmerald.copy(alpha = 0.2f)
                            isSelected -> MummaRose.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val borderColor = when {
                            !state.isAnswered && isSelected -> MummaSecondaryCyan
                            state.isAnswered && isCorrect -> MummaEmerald
                            state.isAnswered && isSelected -> MummaRose
                            else -> MaterialTheme.colorScheme.outline
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(btnColor)
                                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                .clickable(enabled = !state.isAnswered) { onOptionSelected(index) }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${('A' + index)}.",
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isAnswered && isCorrect) MummaEmerald else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Explanation Box
                    AnimatedVisibility(visible = state.isAnswered) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Explanation",
                                    fontWeight = FontWeight.Bold,
                                    color = MummaEmerald,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (state.isAnswered) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MummaSecondaryCyan)
                    ) {
                        Text(
                            text = if (state.currentIndex + 1 < state.data.questions.size) "Next Question" else "View Results",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003544)
                        )
                    }
                }
            }
        }
        is QuizUiState.Completed -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MummaEmerald.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MummaEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Quiz Completed!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You scored ${state.score} out of ${state.total} on ${state.topic}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MummaPrimaryAmber)
                ) {
                    Text("Try Again", color = Color(0xFF1E1402), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onBackToMenu,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Back to Study Menu", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun FlashcardsView(
    state: FlashcardsUiState,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBackToMenu: () -> Unit
) {
    when (state) {
        is FlashcardsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MummaTertiaryViolet)
            }
        }
        is FlashcardsUiState.Active -> {
            if (state.cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No flashcards available for this topic.")
                }
                return
            }

            val card = state.cards[state.currentIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Card ${state.currentIndex + 1} of ${state.cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MummaTertiaryViolet,
                    fontWeight = FontWeight.Bold
                )

                // Flashcard Surface
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, MummaTertiaryViolet.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .clickable { onFlip() }
                        .testTag("flashcard_item"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.isFlipped) "ANSWER" else "QUESTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isFlipped) MummaEmerald else MummaTertiaryViolet,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Text(
                            text = if (state.isFlipped) card.back else card.front,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Tap to flip",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onPrevious,
                        enabled = state.currentIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Previous")
                    }

                    Button(
                        onClick = onNext,
                        enabled = state.currentIndex + 1 < state.cards.size,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MummaTertiaryViolet)
                    ) {
                        Text("Next")
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun RevisionView(
    state: RevisionUiState,
    onSpeakTip: (String) -> Unit,
    onBackToMenu: () -> Unit
) {
    when (state) {
        is RevisionUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MummaEmerald)
            }
        }
        is RevisionUiState.Ready -> {
            val r = state.summary
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Core Definition",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MummaEmerald
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = r.coreDefinition,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Key Takeaways",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MummaSecondaryCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        r.keyPoints.forEach { pt ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text("•", color = MummaSecondaryCyan, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mumma's Study Tip",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MummaPrimaryAmber
                            )
                            IconButton(onClick = { onSpeakTip(r.mummaStudyTip) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak tip",
                                    tint = MummaPrimaryAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${r.mummaStudyTip}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        else -> {}
    }
}
