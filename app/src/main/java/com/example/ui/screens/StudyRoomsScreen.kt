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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.StudyRoomUiState
import com.example.ui.VirtualStudyPeer
import com.example.ui.VirtualStudyRoom
import com.example.ui.theme.AlabasterMist
import com.example.ui.theme.CelestialOpal
import com.example.ui.theme.CelestialOpalContainer
import com.example.ui.theme.CelestialOpalGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianHairlineBorder
import com.example.ui.theme.ObsidianInsetTrough
import com.example.ui.theme.ObsidianSurfaceCard
import com.example.ui.theme.ObsidianSurfaceLow
import com.example.ui.theme.ObsidianVoidCanvas
import com.example.ui.theme.RadiantEmber
import com.example.ui.theme.RadiantEmberContainer
import com.example.ui.theme.RadiantEmberSoft
import com.example.ui.theme.WarmCreamIvory

@Composable
fun StudyRoomsScreen(
    studyRoomState: StudyRoomUiState,
    userFocusedMinutesToday: Int,
    userStreakDays: Int,
    layoutInfo: AdaptiveLayoutInfo,
    onBackToHome: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onToggleRoomTicker: () -> Unit,
    onSendCheer: () -> Unit
) {
    BackHandler(onBack = onBackToHome)

    val activeRoom = studyRoomState.rooms.find { it.id == studyRoomState.joinedRoomId }
        ?: studyRoomState.rooms.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_rooms_list"),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ActiveSanctuaryRoomBanner(
                room = activeRoom,
                studyRoomState = studyRoomState,
                onToggleRoomTicker = onToggleRoomTicker,
                onSendCheer = onSendCheer
            )
        }

        if (layoutInfo.isTwoPaneCapable) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        RoomDirectorySection(
                            rooms = studyRoomState.rooms,
                            joinedRoomId = activeRoom.id,
                            onJoinRoom = onJoinRoom
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RoomLeaderboardSection(
                            room = activeRoom,
                            userFocusedMinutesToday = userFocusedMinutesToday,
                            userStreakDays = userStreakDays
                        )
                    }
                }
            }
        } else {
            item {
                RoomDirectorySection(
                    rooms = studyRoomState.rooms,
                    joinedRoomId = activeRoom.id,
                    onJoinRoom = onJoinRoom
                )
            }
            item {
                RoomLeaderboardSection(
                    room = activeRoom,
                    userFocusedMinutesToday = userFocusedMinutesToday,
                    userStreakDays = userStreakDays
                )
            }
        }
    }
}

@Composable
private fun ActiveSanctuaryRoomBanner(
    room: VirtualStudyRoom,
    studyRoomState: StudyRoomUiState,
    onToggleRoomTicker: () -> Unit,
    onSendCheer: () -> Unit
) {
    val mins = studyRoomState.roomSessionSeconds / 60
    val secs = studyRoomState.roomSessionSeconds % 60
    val formatted = "%02d:%02d".format(mins, secs)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(28.dp), clip = false)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CelestialOpalContainer,
                    modifier = Modifier.border(1.dp, CelestialOpal.copy(alpha = 0.35f), RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CelestialOpalGlow)
                        )
                        Text(
                            text = "${room.activeMembersCount} PEERS IN SILENT RESONANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CelestialOpalGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianInsetTrough,
                    modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                ) {
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = CelestialOpalGlow
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = room.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleRoomTicker,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(52.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = RadiantEmber,
                            spotColor = RadiantEmber
                        )
                        .testTag("room_focus_toggle_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantEmber,
                        contentColor = ObsidianVoidCanvas
                    )
                ) {
                    Icon(
                        imageVector = if (studyRoomState.isInRoomFocus) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (studyRoomState.isInRoomFocus) "Pause Room Study" else "Study Together Now",
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = onSendCheer,
                    modifier = Modifier
                        .weight(0.75f)
                        .height(52.dp)
                        .testTag("room_cheer_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ObsidianSurfaceCard,
                        contentColor = AlabasterMist
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = RadiantEmberSoft
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cheer (${studyRoomState.cheerCount})",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomDirectorySection(
    rooms: List<VirtualStudyRoom>,
    joinedRoomId: String,
    onJoinRoom: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(26.dp), clip = false)
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = null,
                    tint = CelestialOpalGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "SANCTUARY HALLS • OFFLINE CO-PRESENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CelestialOpalGlow,
                    fontWeight = FontWeight.Bold
                )
            }

            rooms.forEach { room ->
                val isJoined = room.id == joinedRoomId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isJoined) ObsidianSurfaceCard else ObsidianInsetTrough)
                        .border(
                            width = 1.dp,
                            color = if (isJoined) CelestialOpal.copy(alpha = 0.45f) else ObsidianHairlineBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onJoinRoom(room.id) }
                        .padding(14.dp)
                        .testTag("join_room_${room.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = room.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = AlabasterMist,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = room.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isJoined) CelestialOpalContainer else ObsidianSurfaceLow
                    ) {
                        Text(
                            text = if (isJoined) "Joined" else "${room.activeMembersCount} Live",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isJoined) CelestialOpalGlow else MutedSlate,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomLeaderboardSection(
    room: VirtualStudyRoom,
    userFocusedMinutesToday: Int,
    userStreakDays: Int
) {
    val userEntry = VirtualStudyPeer(
        id = "you_local",
        name = "You (Rega Sanctuary)",
        countryFlag = "ZEN",
        activityTag = "Mindful Focus",
        focusedMinutesToday = userFocusedMinutesToday,
        streakDays = userStreakDays,
        statusNote = "Offline Sanctuary Active"
    )

    val combinedPeers = (room.peers + userEntry).sortedByDescending { it.focusedMinutesToday }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(26.dp), clip = false)
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = RadiantEmberSoft,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "LIVE COHORT TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadiantEmberSoft,
                    fontWeight = FontWeight.Bold
                )
            }

            combinedPeers.forEachIndexed { index, peer ->
                val isYou = peer.id == "you_local"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isYou) ObsidianSurfaceCard else ObsidianInsetTrough)
                        .border(
                            width = 1.dp,
                            color = if (isYou) RadiantEmber.copy(alpha = 0.40f) else ObsidianHairlineBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == 0) RadiantEmberContainer else CelestialOpalContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index == 0) RadiantEmberSoft else CelestialOpalGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = "${peer.name} · [${peer.countryFlag}]",
                                style = MaterialTheme.typography.titleSmall,
                                color = AlabasterMist,
                                fontWeight = if (isYou) FontWeight.Bold else FontWeight.SemiBold
                            )
                            Text(
                                text = "${peer.activityTag} · ${peer.statusNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${peer.focusedMinutesToday}m",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmCreamIvory,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${peer.streakDays}d streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = CelestialOpalGlow
                        )
                    }
                }
            }
        }
    }
}
