package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.RegainUiState
import com.example.ui.theme.AlabasterMist
import com.example.ui.theme.CelestialOpal
import com.example.ui.theme.CelestialOpalContainer
import com.example.ui.theme.CelestialOpalGlow
import com.example.ui.theme.CobaltIndigo
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianHairlineBorder
import com.example.ui.theme.ObsidianInsetTrough
import com.example.ui.theme.ObsidianSurfaceCard
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ObsidianSurfaceLow
import com.example.ui.theme.ObsidianVoidCanvas
import com.example.ui.theme.RadiantEmber
import com.example.ui.theme.RadiantEmberContainer
import com.example.ui.theme.RadiantEmberSoft
import com.example.ui.theme.WarmCreamIvory

@Composable
fun AiCoachInsightsScreen(
    uiState: RegainUiState,
    singlePromptPreview: String,
    layoutInfo: AdaptiveLayoutInfo,
    onBackToHome: () -> Unit,
    onGenerateDailyInsight: () -> Unit,
    onResetQuotaForDemo: () -> Unit
) {
    BackHandler(onBack = onBackToHome)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_coach_insights_list"),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Cognitive Engine Active + Local Vault Header Row
        item {
            CognitiveEngineHeaderRow()
        }

        if (layoutInfo.isTwoPaneCapable) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.05f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SinglePromptAiCoachCard(
                            uiState = uiState,
                            singlePromptPreview = singlePromptPreview,
                            onGenerateDailyInsight = onGenerateDailyInsight,
                            onResetQuotaForDemo = onResetQuotaForDemo
                        )
                        NeuralTelemetryFeedSection()
                    }
                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WeeklyAttentionDeltaCard(uiState = uiState)
                        ImpulseInterceptionsAuditCard(uiState = uiState)
                        AxiomOfTheDayCard()
                    }
                }
            }
        } else {
            item {
                SinglePromptAiCoachCard(
                    uiState = uiState,
                    singlePromptPreview = singlePromptPreview,
                    onGenerateDailyInsight = onGenerateDailyInsight,
                    onResetQuotaForDemo = onResetQuotaForDemo
                )
            }
            item {
                WeeklyAttentionDeltaCard(uiState = uiState)
            }
            item {
                ImpulseInterceptionsAuditCard(uiState = uiState)
            }
            item {
                NeuralTelemetryFeedSection()
            }
            item {
                AxiomOfTheDayCard()
            }
        }
    }
}

@Composable
private fun CognitiveEngineHeaderRow() {
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
                text = "COGNITIVE ENGINE ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = AlabasterMist,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = ObsidianSurfaceCard,
            modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = WarmCreamIvory,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Local Vault",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmCreamIvory
                )
            }
        }
    }
}

@Composable
private fun SinglePromptAiCoachCard(
    uiState: RegainUiState,
    singlePromptPreview: String,
    onGenerateDailyInsight: () -> Unit,
    onResetQuotaForDemo: () -> Unit
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
            // Top Dual Pills: "1/1 DAILY QUOTA AVAILABLE" + "Offline Cache"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianInsetTrough,
                    modifier = Modifier
                        .weight(1.25f)
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
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
                            text = "$quotaLeft/1 DAILY QUOTA\nAVAILABLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantEmberSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianInsetTrough,
                    modifier = Modifier
                        .weight(0.95f)
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = WarmCreamIvory,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Offline\nCache",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCreamIvory
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Rega AI Attention Coach",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AlabasterMist,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Daily single-prompt cognitive synthesis. Consolidated biometric telemetry and friction logs ready for on-device reasoning.",
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
                        text = "Detected Bottleneck: ${todayInsight.habitBottleneck}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CelestialOpalContainer
                    ) {
                        Text(
                            text = "Projected Time Reclaimed: +${todayInsight.timeSavedProjectionMinutes} mins/week",
                            style = MaterialTheme.typography.labelMedium,
                            color = CelestialOpalGlow,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

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
                                    .size(16.dp)
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

            // Primary Radiant Ember Pill CTA: "Synthesize Daily Focus ->"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onGenerateDailyInsight,
                    enabled = quotaLeft > 0 && !uiState.isGeneratingAiInsight,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .shadow(
                            elevation = if (quotaLeft > 0) 14.dp else 0.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = RadiantEmber,
                            spotColor = RadiantEmber
                        )
                        .testTag("generate_ai_insight_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantEmber,
                        contentColor = ObsidianVoidCanvas,
                        disabledContainerColor = ObsidianSurfaceCard,
                        disabledContentColor = MutedSlate
                    )
                ) {
                    if (uiState.isGeneratingAiInsight) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ObsidianVoidCanvas
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesizing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (quotaLeft > 0) {
                                "Synthesize Daily Focus"
                            } else {
                                "Daily 1/1 Synthesis Cached"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (quotaLeft == 0) {
                    OutlinedButton(
                        onClick = onResetQuotaForDemo,
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("reset_ai_quota_button"),
                        shape = RoundedCornerShape(50),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(ObsidianBorderSubtle)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Daily Quota for Demo",
                            tint = AlabasterMist
                        )
                    }
                }
            }

            // Expandable Recessed Pill: "Inspect Privacy-Safe Prompt Payload"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .clickable { showPromptPayload = !showPromptPayload }
                    .testTag("toggle_prompt_preview_button"),
                shape = RoundedCornerShape(50),
                color = ObsidianInsetTrough
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showPromptPayload) {
                            "Hide Privacy-Safe Prompt Payload"
                        } else {
                            "Inspect Privacy-Safe Prompt Payload"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = AlabasterMist
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (showPromptPayload) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianInsetTrough,
                    modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = singlePromptPreview,
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
private fun WeeklyAttentionDeltaCard(uiState: RegainUiState) {
    val reclaimed = (uiState.userProgress.savedMinutesToday + 24).coerceAtLeast(42)
    val bars = listOf(
        Triple("M", 0.56f, 0.25f),
        Triple("T", 0.44f, 0.34f),
        Triple("W", 0.68f, 0.16f),
        Triple("T", 0.84f, 0.12f),
        Triple("F", 0.54f, 0.28f),
        Triple("S", 0.34f, 0.30f),
        Triple("S", 0.36f, 0.18f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "COGNITIVE RHYTHM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Text(
                        text = "Weekly Attention\nDelta",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianInsetTrough,
                    modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "↗ +${reclaimed}m",
                            style = MaterialTheme.typography.labelLarge,
                            color = RadiantEmberSoft,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reclaimed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate
                        )
                    }
                }
            }

            // 7-Day Stacked Capsule Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = bars.size
                    val slotWidth = size.width / count
                    val barW = (slotWidth * 0.56f).coerceAtMost(28.dp.toPx())
                    val targetY = size.height * 0.22f

                    // Dashed 120m Target Line
                    drawLine(
                        color = MutedSlate.copy(alpha = 0.35f),
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    bars.forEachIndexed { idx, (_, focusFrac, driftFrac) ->
                        val x = idx * slotWidth + (slotWidth - barW) / 2f
                        val totalFrac = (focusFrac + driftFrac).coerceAtMost(0.95f)
                        val totalH = size.height * totalFrac
                        val focusH = size.height * focusFrac

                        // Upper Impulse Drift Capsule (Muted Indigo Slate)
                        drawRoundRect(
                            color = Color(0xFF4B556E),
                            topLeft = Offset(x, size.height - totalH),
                            size = Size(barW, totalH),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )

                        // Lower Deep Focus Capsule (Radiant Ember)
                        drawRoundRect(
                            color = if (idx <= 4) RadiantEmber else RadiantEmber.copy(alpha = 0.6f),
                            topLeft = Offset(x, size.height - focusH),
                            size = Size(barW, focusH),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                bars.forEachIndexed { idx, (day, _, _) ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (idx == 3) AlabasterMist else MutedSlate,
                        fontWeight = if (idx == 3) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Legend Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianInsetTrough)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RadiantEmber)
                    )
                    Text(
                        text = "Deep Focus\n(162m)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlabasterMist
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6B7898))
                    )
                    Text(
                        text = "Impulse Drift\n(22m)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }

                Text(
                    text = "91% FLOW\nRATIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmCreamIvory,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ImpulseInterceptionsAuditCard(uiState: RegainUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TACTILE SHIELD AUDIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Text(
                        text = "Impulse Interceptions",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            uiState.appLimits.forEachIndexed { idx, app ->
                val isOverOrStrict = app.isBlockedCompletely || app.isStrictLocked || app.usedTodayMinutes > app.dailyLimitMinutes
                val ratio = (app.usedTodayMinutes.toFloat() / app.dailyLimitMinutes.coerceAtLeast(1)).coerceIn(0.2f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianInsetTrough)
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            idx == 0 -> RadiantEmberContainer
                                            idx == 1 -> CobaltIndigo.copy(alpha = 0.25f)
                                            else -> ObsidianSurfaceCard
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (idx == 0) RadiantEmber else WarmCreamIvory,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AlabasterMist,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isOverOrStrict) "Strict shield enforced" else "Safe trajectory",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverOrStrict) RadiantEmberSoft else CobaltIndigo
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianSurfaceCard
                        ) {
                            Text(
                                text = "${(app.blockedCountToday + 3)} INTERVENTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadiantEmberSoft,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (isOverOrStrict) RadiantEmber else CobaltIndigo,
                        trackColor = ObsidianSurfaceHigh
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${app.usedTodayMinutes}m recorded · ${app.pickupsToday} pickups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                        Text(
                            text = "Budget: ${app.dailyLimitMinutes}m/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeuralTelemetryFeedSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEURAL TELEMETRY FEED",
                style = MaterialTheme.typography.labelSmall,
                color = AlabasterMist,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "2 Insights Ready",
                style = MaterialTheme.typography.labelSmall,
                color = RadiantEmberSoft,
                fontWeight = FontWeight.Bold
            )
        }

        // Insight Card 1: Dopamine Trigger Pattern
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(RadiantEmberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = RadiantEmberSoft,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Dopamine Trigger Pattern",
                            style = MaterialTheme.typography.titleSmall,
                            color = AlabasterMist,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "14:30 PEAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }

                Text(
                    text = "You consistently unlock entertainment platforms around 2:30 PM post-lunch. Scheduling a 15-minute guided breath pause at 2:15 PM eliminates ~80% of this recurring cognitive drift.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianInsetTrough,
                    modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = RadiantEmberSoft,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Schedule 2:15 PM Breath Guard",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantEmberSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Insight Card 2: Deep Flow Velocity
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CobaltIndigo.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = null,
                                tint = CobaltIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Deep Flow Velocity",
                            style = MaterialTheme.typography.titleSmall,
                            color = AlabasterMist,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "+34% DURATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Your continuous focus intervals expand from 28m to 44m whenever the tactile soundscape \"Kyoto Moss Rain\" is active on binaural 40Hz channel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(ObsidianInsetTrough)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val bars = listOf(8.dp, 14.dp, 18.dp, 10.dp, 16.dp, 12.dp, 18.dp, 10.dp)
                        bars.forEachIndexed { idx, h ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(h)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (idx % 2 == 0) RadiantEmber else WarmCreamIvory)
                            )
                        }
                    }
                    Text(
                        text = "KYOTO RAIN • 40HZ",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmCreamIvory
                    )
                }
            }
        }
    }
}

@Composable
private fun AxiomOfTheDayCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "AXIOM OF THE DAY",
                style = MaterialTheme.typography.labelSmall,
                color = MutedSlate,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "“Attention is the rarest and purest form of generosity.”",
                style = MaterialTheme.typography.headlineSmall,
                color = AlabasterMist,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— Simone Weil",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmCreamIvory
                )
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark Axiom",
                    tint = MutedSlate,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
