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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AvatarState
import com.example.ui.components.MummaAvatar
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
import com.example.ui.viewmodel.MummaViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: MummaViewModel,
    onNavigateToConversation: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val memories by viewModel.memories.collectAsState()
    val memoryCount by viewModel.memoryCount.collectAsState()
    val latestMessage by viewModel.latestMessage.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val selectedStudyTopic by viewModel.selectedStudyTopic.collectAsState()
    val greetingMessage by viewModel.greetingMessage.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()

    val avatarState = when {
        isSpeaking -> AvatarState.SPEAKING
        isListening -> AvatarState.LISTENING
        isThinking -> AvatarState.THINKING
        else -> AvatarState.IDLE
    }

    val greeting = getContextualGreeting()
    val recentMemory = memories.firstOrNull()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ElegantDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Status + App Title + Action/Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ElegantEmerald)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STATUS: READY / OFFLINE-FIRST",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElegantTextMuted,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mumma",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = ElegantTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Memory count pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ElegantDarkSurfaceVariant)
                            .border(1.dp, ElegantDarkCardBorder, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToMemories() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_memory_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Memories",
                                tint = ElegantLavenderPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$memoryCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElegantTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ElegantDarkSurfaceVariant)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ElegantTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Central Companion Avatar & Prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient glow rings
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(ElegantLavenderPrimary.copy(alpha = 0.08f))
                    )
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .clip(CircleShape)
                            .background(ElegantLavenderPrimary.copy(alpha = 0.15f))
                    )
                    MummaAvatar(
                        size = 104.dp,
                        state = avatarState
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Language Switcher Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ElegantDarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val langs = listOf("ENGLISH" to "EN", "HINGLISH" to "Hinglish", "HINDI" to "हिंदी")
                    langs.forEach { (code, label) ->
                        val isSel = currentLanguage == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) ElegantLavenderPrimary else Color.Transparent)
                                .clickable { viewModel.setLanguage(code) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) ElegantDeepViolet else ElegantTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Contextual Maternal Greeting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.speakText(greetingMessage) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = greetingMessage,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = ElegantTextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak Greeting",
                        tint = ElegantLavenderPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Topic: $selectedStudyTopic",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElegantLavenderPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lower Section: Recent Memory Card & 2-Grid Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Recent Memory Card (Design HTML style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigateToMemories() }
                        .testTag("home_recent_memory_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = ElegantDarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ElegantLavenderPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "M",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ElegantDeepViolet
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recent Memory",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ElegantTextPrimary
                                )
                                Text(
                                    text = recentMemory?.content ?: "Prefers quiet night study & Feynman oral drills",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElegantTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElegantLavenderPrimary)
                        )
                    }
                }

                // Grid 2 Columns: TALK and STUDY Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Talk Action Card
                    Button(
                        onClick = onNavigateToConversation,
                        modifier = Modifier
                            .weight(1f)
                            .height(116.dp)
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                            .testTag("talk_to_mumma_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantLavenderPrimary,
                            contentColor = ElegantDeepViolet
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = ElegantDeepViolet,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TALK",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = ElegantDeepViolet
                            )
                        }
                    }

                    // Study Action Card
                    Button(
                        onClick = onNavigateToStudy,
                        modifier = Modifier
                            .weight(1f)
                            .height(116.dp)
                            .border(1.dp, ElegantDarkCardBorder, RoundedCornerShape(24.dp))
                            .testTag("study_with_mumma_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantDeepViolet,
                            contentColor = ElegantLavenderPrimary
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = ElegantLavenderPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "STUDY",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = ElegantLavenderPrimary
                            )
                        }
                    }
                }

                // Recent Chat Preview Card if exists
                if (latestMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, ElegantDarkCardBorder, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToConversation() }
                            .testTag("recent_chat_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = ElegantDarkSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ElegantDarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Recent chat",
                                    tint = ElegantLavenderPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (latestMessage?.role == "user") "You" else "Mumma",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (latestMessage?.role == "user") ElegantLavenderPrimary else ElegantTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = latestMessage?.content ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElegantTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun getContextualGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night study"
    }
}
