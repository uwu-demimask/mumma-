package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AvatarState
import com.example.ui.components.MummaAvatar
import com.example.ui.components.TeachAnalysisCard
import com.example.ui.theme.MummaEmerald
import com.example.ui.theme.MummaPrimaryAmber
import com.example.ui.theme.MummaRose
import com.example.ui.theme.MummaSecondaryCyan
import com.example.ui.viewmodel.MummaViewModel
import com.example.ui.viewmodel.TeachUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachMummaScreen(
    topic: String,
    viewModel: MummaViewModel,
    onBack: () -> Unit,
    onNavigateToChatWithQuestion: (String) -> Unit
) {
    val teachState by viewModel.teachState.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val audioLevel by viewModel.audioRmsDb.collectAsState()

    var manualText by remember { mutableStateOf("") }
    var isManualTyping by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Teach Mumma",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Topic: $topic",
                            style = MaterialTheme.typography.labelSmall,
                            color = MummaPrimaryAmber
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.resetTeachMode()
                            onBack()
                        },
                        modifier = Modifier.testTag("teach_mumma_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (teachState is TeachUiState.AnalysisReady) {
                        IconButton(onClick = { viewModel.resetTeachMode() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry Teaching",
                                tint = MummaSecondaryCyan
                            )
                        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (val state = teachState) {
                    is TeachUiState.Idle, is TeachUiState.RecordingExplanation -> {
                        // Avatar Header
                        MummaAvatar(
                            size = 110.dp,
                            state = if (isListening) AvatarState.LISTENING else AvatarState.TEACHING,
                            audioLevel = audioLevel
                        )

                        Text(
                            text = "Explain \"$topic\" to Mumma",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Rules of the Feynman Technique:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MummaEmerald
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. Don't look at your textbook or notes.\n2. Explain from memory in simple, natural words.\n3. Mumma will analyze your depth, missing concepts, and clarity.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Explanation Input / Voice Recording Area
                        if (!isManualTyping) {
                            val liveText = if (state is TeachUiState.RecordingExplanation) state.partialText else ""

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        1.dp,
                                        if (isListening) MummaSecondaryCyan else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(18.dp)
                                    ),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (liveText.isNotBlank()) {
                                        Text(
                                            text = liveText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = if (isListening) "Listening to your explanation… speak continuously" else "Tap the microphone below and explain $topic orally",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = manualText,
                                onValueChange = { manualText = it },
                                placeholder = { Text("Type your explanation of $topic here...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .testTag("teach_explanation_text_field"),
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MummaSecondaryCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        // Primary Action Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!isManualTyping) {
                                Button(
                                    onClick = {
                                        if (isListening) {
                                            viewModel.stopTeachRecordingAndAnalyze()
                                        } else {
                                            viewModel.startTeachRecording()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("teach_mic_record_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isListening) MummaRose else MummaPrimaryAmber,
                                        contentColor = if (isListening) Color.White else Color(0xFF1E1402)
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isListening) "Finish & Analyze" else "Speak Explanation",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.stopTeachRecordingAndAnalyze(manualText)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("teach_submit_manual_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MummaPrimaryAmber)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF1E1402))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze Explanation", color = Color(0xFF1E1402), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Toggle Typing vs Speaking fallback
                        Text(
                            text = if (isManualTyping) "Switch to Voice Recording" else "Or type explanation manually",
                            style = MaterialTheme.typography.labelMedium,
                            color = MummaSecondaryCyan,
                            modifier = Modifier
                                .clickable { isManualTyping = !isManualTyping }
                                .padding(8.dp)
                        )
                    }

                    is TeachUiState.Analyzing -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            MummaAvatar(
                                size = 120.dp,
                                state = AvatarState.THINKING
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Mumma is analyzing your explanation…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Checking concept coverage, clarity, and factual accuracy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            CircularProgressIndicator(color = MummaPrimaryAmber)
                        }
                    }

                    is TeachUiState.AnalysisReady -> {
                        TeachAnalysisCard(
                            analysis = state.result,
                            onSpeakSummary = { viewModel.speakText(state.result.summaryMessage) },
                            onFollowUpQuestionClicked = { question ->
                                onNavigateToChatWithQuestion(question)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.resetTeachMode() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("teach_another_topic_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MummaEmerald)
                        ) {
                            Text("Teach Another Topic", color = Color(0xFF032617), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
