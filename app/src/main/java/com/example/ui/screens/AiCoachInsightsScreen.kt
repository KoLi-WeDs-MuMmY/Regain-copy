package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.ChatMessage
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.AiCoachMode
import com.example.ui.RegainUiState
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiCoachInsightsScreen(
    uiState: RegainUiState,
    singlePromptPreview: String,
    layoutInfo: AdaptiveLayoutInfo,
    onBackToHome: () -> Unit,
    onGenerateDailyInsight: () -> Unit,
    onSetMode: (AiCoachMode) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onToggleApiKeyDialog: (Boolean) -> Unit
) {
    BackHandler {
        if (uiState.aiCoachState.mode == AiCoachMode.CHAT) {
            onSetMode(AiCoachMode.INSIGHTS)
        } else {
            onBackToHome()
        }
    }

    if (uiState.aiCoachState.showApiKeyDialog) {
        ApiKeySettingsDialog(
            currentKey = uiState.userProgress.customGeminiApiKey,
            onDismiss = { onToggleApiKeyDialog(false) },
            onSave = { onUpdateApiKey(it); onToggleApiKeyDialog(false) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoidCanvas)
    ) {
        // 1. Cognitive Engine Header
        Box(modifier = Modifier.padding(horizontal = layoutInfo.horizontalPadding, vertical = 12.dp)) {
            CognitiveEngineHeaderRow(onOpenSettings = { onToggleApiKeyDialog(true) })
        }

        // 2. Mode Selector Pill Bar
        Box(modifier = Modifier.padding(horizontal = layoutInfo.horizontalPadding, vertical = 8.dp)) {
            AiModeSelector(
                currentMode = uiState.aiCoachState.mode,
                onSetMode = onSetMode
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.aiCoachState.mode) {
                AiCoachMode.INSIGHTS -> {
                    InsightsList(
                        uiState = uiState,
                        singlePromptPreview = singlePromptPreview,
                        layoutInfo = layoutInfo,
                        onGenerateDailyInsight = onGenerateDailyInsight
                    )
                }
                AiCoachMode.CHAT -> {
                    ChatInterface(
                        uiState = uiState,
                        layoutInfo = layoutInfo,
                        onSendMessage = onSendChatMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun CognitiveEngineHeaderRow(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RadiantEmber)
            )
            Text(
                text = "REGA AI COACH ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = AlabasterMist,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ObsidianSurfaceCard)
                .border(1.dp, ObsidianHairlineBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "AI Settings",
                tint = WarmCreamIvory,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AiModeSelector(
    currentMode: AiCoachMode,
    onSetMode: (AiCoachMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(ObsidianSurfaceLow)
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        AiCoachMode.entries.forEach { mode ->
            val selected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) ObsidianSurfaceHigh else Color.Transparent)
                    .clickable { onSetMode(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) AlabasterMist else MutedSlate,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InsightsList(
    uiState: RegainUiState,
    singlePromptPreview: String,
    layoutInfo: AdaptiveLayoutInfo,
    onGenerateDailyInsight: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SinglePromptAiCoachCard(
                uiState = uiState,
                singlePromptPreview = singlePromptPreview,
                onGenerateDailyInsight = onGenerateDailyInsight
            )
        }
        item {
            NeuralTelemetryFeedSection()
        }
    }
}

@Composable
private fun ChatInterface(
    uiState: RegainUiState,
    layoutInfo: AdaptiveLayoutInfo,
    onSendMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.aiCoachState.chatHistory.size) {
        if (uiState.aiCoachState.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(uiState.aiCoachState.chatHistory.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.aiCoachState.chatHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = RadiantEmberSoft.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Start a mindful conversation with Rega",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = layoutInfo.horizontalPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.aiCoachState.chatHistory) { message ->
                    ChatBubble(message)
                }
                if (uiState.aiCoachState.isSendingChat) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = RadiantEmber
                            )
                        }
                    }
                }
            }
        }

        // Message Input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ObsidianSurfaceLow,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Rega for focus advice...", color = MutedSlate) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AlabasterMist,
                        unfocusedTextColor = AlabasterMist,
                        focusedBorderColor = RadiantEmberSoft,
                        unfocusedBorderColor = ObsidianHairlineBorder,
                        cursorColor = RadiantEmber
                    ),
                    maxLines = 3
                )

                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) ObsidianSurfaceCard else ObsidianInsetTrough,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) RadiantEmber.copy(alpha = 0.2f) else ObsidianHairlineBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) AlabasterMist else WarmCreamIvory
            )
        }
    }
}

@Composable
private fun ApiKeySettingsDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var keyText by remember { mutableStateOf(currentKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini API Settings", color = AlabasterMist) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter your personal Google Gemini API key to enable full chat and high-frequency insights (15 RPM free tier supported).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AlabasterMist,
                        unfocusedTextColor = AlabasterMist
                    )
                )
                Text(
                    "Note: Your key is stored locally on this device only.",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadiantEmberSoft
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyText) },
                colors = ButtonDefaults.buttonColors(containerColor = RadiantEmber)
            ) {
                Text("Save Key", color = ObsidianVoidCanvas)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlate)
            }
        },
        containerColor = ObsidianSurfaceLow
    )
}

@Composable
private fun SinglePromptAiCoachCard(
    uiState: RegainUiState,
    singlePromptPreview: String,
    onGenerateDailyInsight: () -> Unit
) {
    var showPromptPayload by remember { mutableStateOf(false) }
    val todayInsight = uiState.todayAiInsight
    val quotaLeft = uiState.dailyAiRequestsRemaining

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianInsetTrough,
                    modifier = Modifier.weight(1f).border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = RadiantEmberSoft,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (uiState.userProgress.customGeminiApiKey.isNotBlank()) "CUSTOM KEY ACTIVE" else "DAILY QUOTA: $quotaLeft/1",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantEmberSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Daily Focus Synthesis",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlabasterMist,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Consolidated biometric telemetry and focus logs analyzed by Rega AI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            }

            if (todayInsight != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianInsetTrough)
                        .border(1.dp, CelestialOpal.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = todayInsight.coachHeadline,
                        style = MaterialTheme.typography.titleMedium,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bottleneck: ${todayInsight.habitBottleneck}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    
                    listOf(todayInsight.microChallenge1, todayInsight.microChallenge2, todayInsight.microChallenge3).forEach { step ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CelestialOpalGlow, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                            Text(text = step, style = MaterialTheme.typography.bodySmall, color = AlabasterMist)
                        }
                    }
                }
            }

            Button(
                onClick = onGenerateDailyInsight,
                enabled = (quotaLeft > 0 || uiState.userProgress.customGeminiApiKey.isNotBlank()) && !uiState.isGeneratingAiInsight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(12.dp, RoundedCornerShape(50), clip = false, ambientColor = RadiantEmber)
                    .testTag("generate_ai_insight_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = RadiantEmber, contentColor = ObsidianVoidCanvas)
            ) {
                if (uiState.isGeneratingAiInsight) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianVoidCanvas)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...")
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synthesize Daily Focus", fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = ObsidianInsetTrough,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                    .clickable { showPromptPayload = !showPromptPayload }
                    .testTag("inspect_prompt_payload_toggle")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inspect Privacy-Safe Prompt Payload",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlate
                    )
                    Icon(
                        imageVector = if (showPromptPayload) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showPromptPayload) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianVoidCanvas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = todayInsight?.promptUsedPreview ?: singlePromptPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NeuralTelemetryFeedSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "NEURAL TELEMETRY FEED",
            style = MaterialTheme.typography.labelSmall,
            color = AlabasterMist,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = RadiantEmberSoft, modifier = Modifier.size(18.dp))
                    Text(text = "Dopamine Trigger Pattern", style = MaterialTheme.typography.titleSmall, color = AlabasterMist, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "You consistently unlock entertainment platforms around 2:30 PM. Scheduling a breath pause at 2:15 PM is recommended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }
        }
    }
}
