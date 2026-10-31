package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal allowing any modular slot inside [BlockerOverlayContainer] to request
 * a coordinated animated dismissal without coupling presentation UI to motion mechanics.
 */
val LocalBlockerOverlayDismissTrigger = compositionLocalOf<() -> Unit> { {} }

/**
 * Clean, decoupled Jetpack Compose animation container for the app blocker intercept.
 *
 * Architecture & Motion Mechanics:
 * - Entry Transition: `fadeIn(animationSpec = tween(250))` combined with
 *   `scaleIn(initialScale = 0.95f, animationSpec = tween(250))` and
 *   `slideInVertically(animationSpec = tween(250), initialOffsetY = { it / 4 })`.
 * - Exit Transition: `fadeOut(animationSpec = tween(200))` combined with
 *   `slideOutVertically(animationSpec = tween(200), targetOffsetY = { it / 4 })`.
 * - State Coordination: Controlled via internal boolean state [isVisible] and [MutableTransitionState].
 *   On dismissal, the container waits for the 200ms exit transition to finish completely before
 *   invoking [onDismissComplete] (e.g., routing to `Intent.CATEGORY_HOME`).
 * - Zero hardcoded UI cards, icons, or text inside the container — presentation is strictly
 *   delegated to [content].
 */
@Composable
fun BlockerOverlayContainer(
    onDismissComplete: () -> Unit,
    modifier: Modifier = Modifier,
    dismissRequested: Boolean = false,
    content: @Composable () -> Unit
) {
    val latestOnDismissComplete by rememberUpdatedState(onDismissComplete)
    var isVisible by remember { mutableStateOf(false) }
    var hasEntered by remember { mutableStateOf(false) }
    val transitionState = remember { MutableTransitionState(false) }

    val triggerDismiss: () -> Unit = remember {
        {
            if (isVisible) {
                isVisible = false
                transitionState.targetState = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
        hasEntered = true
        transitionState.targetState = true
    }

    LaunchedEffect(dismissRequested) {
        if (dismissRequested && isVisible) {
            isVisible = false
            transitionState.targetState = false
        }
    }

    LaunchedEffect(hasEntered, isVisible, transitionState.isIdle, transitionState.currentState) {
        if (hasEntered && !isVisible && transitionState.isIdle && !transitionState.currentState) {
            latestOnDismissComplete()
        }
    }

    CompositionLocalProvider(LocalBlockerOverlayDismissTrigger provides triggerDismiss) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xD9090D0B))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("blocker_overlay_container"),
            contentAlignment = Alignment.Center
        ) {
            val maxSlotWidth = if (maxWidth >= 600.dp) 480.dp else 440.dp

            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
                    scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(durationMillis = 250)
                    ) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 250),
                        initialOffsetY = { it / 4 }
                    ),
                exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                    slideOutVertically(
                        animationSpec = tween(durationMillis = 200),
                        targetOffsetY = { it / 4 }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = maxSlotWidth),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Overload accepting a [visible] state flag and passing `requestDismiss` directly to [content].
 */
@Composable
fun BlockerOverlayContainer(
    visible: Boolean,
    onDismissComplete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (requestDismiss: () -> Unit) -> Unit
) {
    if (!visible) return

    var dismissRequested by remember { mutableStateOf(false) }

    BlockerOverlayContainer(
        onDismissComplete = onDismissComplete,
        modifier = modifier,
        dismissRequested = dismissRequested
    ) {
        val trigger = LocalBlockerOverlayDismissTrigger.current
        content {
            dismissRequested = true
            trigger()
        }
    }
}

/**
 * Routes the user cleanly to the Android Home screen (`Intent.CATEGORY_HOME`) after
 * [BlockerOverlayContainer]'s exit animation has finished.
 */
fun routeToAndroidHomeScreen(context: Context) {
    try {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    } catch (_: Exception) {
    }
}
