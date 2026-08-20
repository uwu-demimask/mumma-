package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.MessageEntity
import com.example.ui.components.AvatarState
import com.example.ui.components.MummaAvatar
import com.example.ui.components.VoiceControlBar
import com.example.ui.theme.ElegantDarkBackground
import com.example.ui.theme.ElegantDarkCardBorder
import com.example.ui.theme.ElegantDarkSurface
import com.example.ui.theme.ElegantDarkSurfaceVariant
import com.example.ui.theme.ElegantDeepViolet
import com.example.ui.theme.ElegantEmerald
import com.example.ui.theme.ElegantLavenderPrimary
import com.example.ui.theme.ElegantTextMuted
import com.example.ui.theme.ElegantTextPrimary
import com.example.ui.theme.ElegantTextSecondary
import com.example.ui.theme.ElegantVioletContainer
import com.example.ui.viewmodel.MummaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: MummaViewModel,
    onBack: () -> Unit,
    onNavigateToMemories: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.currentInputText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val audioLevel by viewModel.audioRmsDb.collectAsState()
    val memoryCount by viewModel.memoryCount.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MummaAvatar(
                            size = 36.dp,
                            state = when {
                                isSpeaking -> AvatarState.SPEAKING
                                isListening -> AvatarState.LISTENING
                                isThinking -> AvatarState.THINKING
                                else -> AvatarState.IDLE
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Mumma",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElegantTextPrimary
                            )
                            Text(
                                text = when {
                                    isListening -> "Listening to you…"
                                    isSpeaking -> "Speaking…"
                                    isThinking -> "Thinking…"
                                    else -> "Ready • Local Memory Active"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isListening) ElegantLavenderPrimary else if (isSpeaking) ElegantLavenderPrimary else ElegantEmerald
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("conversation_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElegantTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMemories, modifier = Modifier.testTag("conversation_memory_button")) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Memories",
                                tint = ElegantLavenderPrimary
                            )
                            if (memoryCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(ElegantLavenderPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$memoryCount",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElegantDeepViolet
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.clearConversationHistory() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = ElegantTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ElegantDarkBackground
                )
            )
        },
        bottomBar = {
            VoiceControlBar(
                inputText = inputText,
                onTextChanged = viewModel::onInputTextChanged,
                onSendClicked = { viewModel.sendMessage() },
                isListening = isListening,
                isSpeaking = isSpeaking,
                isThinking = isThinking,
                audioLevel = audioLevel,
                onStartVoice = { viewModel.startVoiceInput() },
                onStopVoice = { viewModel.stopVoiceInput() },
                onStopSpeaking = { viewModel.stopSpeaking() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ElegantDarkBackground)
        ) {
            // Quick suggestions row when starting
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    MummaAvatar(
                        size = 90.dp,
                        state = AvatarState.IDLE
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Talk to Mumma naturally",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElegantTextPrimary
                    )
                    Text(
                        text = "Ask questions, teach topics orally, or store memory preferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElegantTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    SuggestionChips(
                        onSelectSuggestion = { suggestion ->
                            viewModel.sendMessage(suggestion)
                        }
                    )
                }
            }

            // Message Bubble List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onReplayAudio = { viewModel.speakText(message.content) }
                    )
                }

                if (isThinking) {
                    item {
                        ThinkingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    onReplayAudio: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElegantLavenderPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ElegantDeepViolet
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) ElegantLavenderPrimary else ElegantDarkSurfaceVariant,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) Color.Transparent else ElegantDarkCardBorder,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) ElegantDeepViolet else ElegantTextPrimary
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onReplayAudio,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen",
                                tint = ElegantTextSecondary,
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
private fun ThinkingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ElegantVioletContainer),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = ElegantLavenderPrimary,
                strokeWidth = 2.dp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Mumma is thinking…",
            style = MaterialTheme.typography.bodySmall,
            color = ElegantTextSecondary
        )
    }
}

@Composable
private fun SuggestionChips(
    onSelectSuggestion: (String) -> Unit
) {
    val suggestions = listOf(
        "Remember that I prefer studying at night",
        "Remember that I struggle with organic chemistry",
        "What do you remember about me?",
        "Explain Photosynthesis to me in simple terms",
        "How can I practice active recall today?"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElegantDarkSurfaceVariant)
                    .border(1.dp, ElegantDarkCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onSelectSuggestion(prompt) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = ElegantTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
