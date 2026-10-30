package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Spa
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.StudyRoomUiState
import com.example.ui.VirtualStudyPeer
import com.example.ui.VirtualStudyRoom

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
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column {
                        Text(
                            text = room.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${room.activeMembersCount} companions focusing now · ${room.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
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
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("room_focus_toggle_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (studyRoomState.isInRoomFocus) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (studyRoomState.isInRoomFocus) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (studyRoomState.isInRoomFocus) "Pause Room Study" else "Study Together Now"
                    )
                }

                FilledTonalButton(
                    onClick = onSendCheer,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp)
                        .testTag("room_cheer_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cheer (${studyRoomState.cheerCount})")
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Study Sanctuary Rooms (Offline Mode)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Switch between themed focus halls with simulated global study companions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            rooms.forEach { room ->
                val isJoined = room.id == joinedRoomId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isJoined) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = room.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isJoined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = if (isJoined) "Joined" else "${room.activeMembersCount} Live",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isJoined) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Room Focus Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            combinedPeers.forEachIndexed { index, peer ->
                val isYou = peer.id == "you_local"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isYou) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    if (index == 0) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.primary
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = "${peer.name} · [${peer.countryFlag}]",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isYou) FontWeight.Bold else FontWeight.SemiBold
                            )
                            Text(
                                text = "${peer.activityTag} · ${peer.statusNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${peer.focusedMinutesToday}m",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${peer.streakDays}d streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
