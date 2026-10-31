package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiProvider
import com.example.ai.ChatMessage
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.AiCoachMode
import com.example.ui.RegainUiState
import com.example.ui.theme.AlabasterMist
import com.example.ui.theme.CelestialOpal
import com.example.ui.theme.CelestialOpalGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianHairlineBorder
import com.example.ui.theme.ObsidianInsetTrough
import com.example.ui.theme.ObsidianSurfaceCard
import com.example.ui.theme.ObsidianSurfaceLow
import com.example.ui.theme.ObsidianVoidCanvas
import com.example.ui.theme.RadiantEmber
import com.example.ui.theme.RadiantEmberSoft
import com.example.ui.theme.WarmCreamIvory
import com.example.ui.theme.tactile3DCard
import com.example.ui.theme.tactileRecessedWell

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
    onUpdateProviderAndKey: (String, String) -> Unit = { _, key -> onUpdateApiKey(key) },
    onToggleApiKeyDialog: (Boolean) -> Unit
) {
    BackHandler {
        if (uiState.aiCoachState.mode == AiCoachMode.CHAT) {
            onSetMode(AiCoachMode.INSIGHTS)
        } else {
            onBackToHome()
        }
    }

    val activeProvider = AiProvider.resolveEffectiveProvider(
        selectedId = uiState.userProgress.aiProvider,
        apiKey = uiState.userProgress.customGeminiApiKey
    )

    if (uiState.aiCoachState.showApiKeyDialog) {
        ApiKeySettingsDialog(
            currentProviderId = activeProvider.id,
            currentKey = uiState.userProgress.customGeminiApiKey,
            onDismiss = { onToggleApiKeyDialog(false) },
            onSave = { providerId, key ->
                onUpdateProviderAndKey(providerId, key)
                onToggleApiKeyDialog(false)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoidCanvas)
    ) {
        // 1. Cognitive Engine Header with Active Free Provider Pill
        Box(modifier = Modifier.padding(horizontal = layoutInfo.horizontalPadding, vertical = 10.dp)) {
            CognitiveEngineHeaderRow(
                activeProvider = activeProvider,
                hasKey = uiState.userProgress.customGeminiApiKey.isNotBlank(),
                onOpenSettings = { onToggleApiKeyDialog(true) }
            )
        }

        // 2. 3D Tactile Mode Selector Pill Bar
        Box(modifier = Modifier.padding(horizontal = layoutInfo.horizontalPadding, vertical = 6.dp)) {
            AiModeSelector(
                currentMode = uiState.aiCoachState.mode,
                onSetMode = onSetMode
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.aiCoachState.mode) {
                AiCoachMode.INSIGHTS -> {
                    InsightsList(
                        uiState = uiState,
                        activeProvider = activeProvider,
                        singlePromptPreview = singlePromptPreview,
                        layoutInfo = layoutInfo,
                        onGenerateDailyInsight = onGenerateDailyInsight,
                        onOpenProviderSettings = { onToggleApiKeyDialog(true) }
                    )
                }
                AiCoachMode.CHAT -> {
                    ChatInterface(
                        uiState = uiState,
                        activeProvider = activeProvider,
                        layoutInfo = layoutInfo,
                        onSendMessage = onSendChatMessage,
                        onOpenProviderSettings = { onToggleApiKeyDialog(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CognitiveEngineHeaderRow(
    activeProvider: AiProvider,
    hasKey: Boolean,
    onOpenSettings: () -> Unit
) {
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
                    .background(if (hasKey) CelestialOpalGlow else RadiantEmber)
            )
            Text(
                text = if (hasKey) {
                    "REGA AI • ${activeProvider.id} FREE TIER"
                } else {
                    "REGA AI COACH • OFFLINE / MULTI-API"
                },
                style = MaterialTheme.typography.labelSmall,
                color = AlabasterMist,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = ObsidianSurfaceCard,
            modifier = Modifier
                .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                .clickable(onClick = onOpenSettings)
                .testTag("ai_provider_settings_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Configure AI Provider & API Key",
                    tint = RadiantEmberSoft,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (hasKey) activeProvider.id else "API Providers",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmCreamIvory,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MutedSlate,
                    modifier = Modifier.size(14.dp)
                )
            }
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
            .tactileRecessedWell(cornerRadius = 50.dp)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AiCoachMode.entries.forEach { mode ->
            val selected = currentMode == mode
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(8.dp, shape, clip = false, ambientColor = RadiantEmber.copy(alpha = 0.3f))
                                .clip(shape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF282528), Color(0xFF1C1A1D))
                                    )
                                )
                                .border(1.dp, RadiantEmberSoft.copy(alpha = 0.4f), shape)
                        } else {
                            Modifier.clip(shape)
                        }
                    )
                    .clickable { onSetMode(mode) }
                    .padding(vertical = 10.dp)
                    .testTag("ai_mode_tab_${mode.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (mode == AiCoachMode.INSIGHTS) "DAILY SYNTHESIS" else "REGA AI CHAT",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) RadiantEmberSoft else MutedSlate,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InsightsList(
    uiState: RegainUiState,
    activeProvider: AiProvider,
    singlePromptPreview: String,
    layoutInfo: AdaptiveLayoutInfo,
    onGenerateDailyInsight: () -> Unit,
    onOpenProviderSettings: () -> Unit
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
                activeProvider = activeProvider,
                singlePromptPreview = singlePromptPreview,
                onGenerateDailyInsight = onGenerateDailyInsight,
                onOpenProviderSettings = onOpenProviderSettings
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
    activeProvider: AiProvider,
    layoutInfo: AdaptiveLayoutInfo,
    onSendMessage: (String) -> Unit,
    onOpenProviderSettings: () -> Unit
) {
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var messageText by remember { mutableStateOf("") }
    val hasKey = uiState.userProgress.customGeminiApiKey.isNotBlank()

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
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactile3DCard(cornerRadius = 28.dp)
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = RadiantEmberSoft,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = "Chat with Rega AI Coach",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasKey) {
                            "Connected to ${activeProvider.displayName} (${activeProvider.defaultModelLabel}). Ask about Pomodoro focus strategies, breaking dopamine loops, or study planning."
                        } else {
                            "Supports 100% free API keys from Groq Cloud (Llama 3.3 70B), OpenRouter (:free models), and Google Gemini—no billing account required."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF282426),
                        modifier = Modifier
                            .border(1.dp, RadiantEmberSoft.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .clickable(onClick = onOpenProviderSettings)
                    ) {
                        Text(
                            text = if (hasKey) "Switch Provider (${activeProvider.displayName})" else "Configure Free Groq / OpenRouter / Gemini Key",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantEmberSoft,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
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

        // Message Input Bar
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input_field"),
                    placeholder = {
                        Text(
                            text = "Ask Rega (${activeProvider.id} Free Tier)...",
                            color = MutedSlate
                        )
                    },
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
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_chat_send_button"),
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
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) ObsidianSurfaceCard else ObsidianInsetTrough,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
                .border(
                    width = 1.dp,
                    color = if (isUser) RadiantEmber.copy(alpha = 0.3f) else ObsidianHairlineBorder,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
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
    currentProviderId: String,
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedProvider by remember {
        mutableStateOf(AiProvider.fromId(currentProviderId))
    }
    var keyText by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Multi-Provider AI Settings",
                color = WarmCreamIvory,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose a free-tier AI provider for Rega Chat & Daily Insights. Groq and OpenRouter offer 100% free keys with no billing required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )

                // Provider Selector Cards
                AiProvider.entries.forEach { provider ->
                    val isSelected = selectedProvider == provider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ObsidianSurfaceCard else ObsidianInsetTrough)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) RadiantEmberSoft.copy(alpha = 0.55f) else ObsidianHairlineBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedProvider = provider }
                            .padding(12.dp)
                            .testTag("select_ai_provider_${provider.id.lowercase()}"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Default.RadioButtonChecked
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) RadiantEmber else MutedSlate,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WarmCreamIvory,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) RadiantEmber.copy(alpha = 0.2f) else ObsidianSurfaceLow
                            ) {
                                Text(
                                    text = provider.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) RadiantEmberSoft else MutedSlate,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = provider.helpSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate,
                            fontSize = 11.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = keyText,
                    onValueChange = {
                        keyText = it
                        // Auto-select matching provider when pasting a prefixed key
                        when {
                            it.trim().startsWith("gsk_") -> selectedProvider = AiProvider.GROQ
                            it.trim().startsWith("sk-or-") -> selectedProvider = AiProvider.OPENROUTER
                            it.trim().startsWith("AIza") -> selectedProvider = AiProvider.GEMINI
                        }
                    },
                    label = { Text("${selectedProvider.displayName} Key") },
                    placeholder = { Text(selectedProvider.keyPrefixHint, color = MutedSlate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_api_key_input"),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AlabasterMist,
                        unfocusedTextColor = AlabasterMist,
                        focusedBorderColor = RadiantEmberSoft,
                        unfocusedBorderColor = ObsidianHairlineBorder
                    )
                )

                Text(
                    text = "Model: ${selectedProvider.defaultModelLabel} • Stored locally on device only.",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadiantEmberSoft
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedProvider.id, keyText) },
                colors = ButtonDefaults.buttonColors(containerColor = RadiantEmber),
                modifier = Modifier.testTag("save_ai_provider_key_button")
            ) {
                Text("Save Provider & Key", color = ObsidianVoidCanvas, fontWeight = FontWeight.Bold)
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
    activeProvider: AiProvider,
    singlePromptPreview: String,
    onGenerateDailyInsight: () -> Unit,
    onOpenProviderSettings: () -> Unit
) {
    var showPromptPayload by remember { mutableStateOf(false) }
    val todayInsight = uiState.todayAiInsight
    val quotaLeft = uiState.dailyAiRequestsRemaining
    val hasCustomKey = uiState.userProgress.customGeminiApiKey.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DCard(
                cornerRadius = 30.dp,
                elevation = 14.dp,
                topColor = Color(0xFF1B191C),
                bottomColor = Color(0xFF131215),
                accentGlow = RadiantEmber.copy(alpha = 0.16f)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .tactileRecessedWell(cornerRadius = 50.dp)
                        .clickable(onClick = onOpenProviderSettings)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
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
                            text = if (hasCustomKey) {
                                "${activeProvider.id} FREE TIER ACTIVE"
                            } else {
                                "DAILY QUOTA: $quotaLeft/1 • TAP FOR GROQ/OPENROUTER"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantEmberSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Rega AI Attention Coach",
                    style = MaterialTheme.typography.headlineSmall,
                    color = WarmCreamIvory,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Daily single-prompt cognitive synthesis powered by Groq Llama 3.3, OpenRouter free models, Google Gemini, or the On-Device Zen Engine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            }

            if (todayInsight != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileRecessedWell(cornerRadius = 20.dp)
                        .border(1.dp, CelestialOpal.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = todayInsight.coachHeadline,
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bottleneck: ${todayInsight.habitBottleneck}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )

                    listOf(
                        todayInsight.microChallenge1,
                        todayInsight.microChallenge2,
                        todayInsight.microChallenge3
                    ).forEach { step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CelestialOpalGlow,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlabasterMist
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onGenerateDailyInsight,
                enabled = (quotaLeft > 0 || hasCustomKey) && !uiState.isGeneratingAiInsight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(16.dp, RoundedCornerShape(50), clip = false, ambientColor = RadiantEmber)
                    .testTag("generate_ai_insight_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                )
            ) {
                if (uiState.isGeneratingAiInsight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ObsidianVoidCanvas
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synthesizing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synthesize Daily Focus", fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileRecessedWell(cornerRadius = 50.dp)
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
            color = WarmCreamIvory,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .tactile3DCard(
                    cornerRadius = 26.dp,
                    elevation = 10.dp,
                    topColor = Color(0xFF1A191D),
                    bottomColor = Color(0xFF121115)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = RadiantEmberSoft,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Dopamine Trigger Pattern",
                        style = MaterialTheme.typography.titleSmall,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "You consistently unlock entertainment platforms around 2:30 PM. Scheduling a breath pause at 2:15 PM eliminates ~80% of afternoon cognitive drift.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }
        }
    }
}
