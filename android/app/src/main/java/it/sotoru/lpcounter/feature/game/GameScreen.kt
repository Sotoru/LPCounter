package it.sotoru.lpcounter.feature.game

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.accessibility.AccessibilityManager
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.data.Settings
import it.sotoru.lpcounter.feature.chance.Chance
import it.sotoru.lpcounter.feature.chance.ChanceButtons
import it.sotoru.lpcounter.feature.chance.ChanceOverlay
import it.sotoru.lpcounter.feature.timer.TimerReadout
import it.sotoru.lpcounter.ui.led.LedMatrix
import it.sotoru.lpcounter.ui.reduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Full-screen duel pushed from Home (SPEC §4): no bars, no back gesture; leaving is the Home button. */
@Composable
fun GameScreen(state: GameState, settings: Settings, resets: Int, viewModel: GameViewModel, onHome: () -> Unit) {
    // Equivalent of UIApplication.isIdleTimerDisabled: the table keeps the screen awake.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // On phones the duel is always landscape (SPEC §4); tablets lay out by window shape. The Home is
    // unlocked again when the game leaves composition.
    val activity = LocalActivity.current
    val isPhone = LocalConfiguration.current.smallestScreenWidthDp < 600
    DisposableEffect(activity, isPhone) {
        if (isPhone) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if (isPhone) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    var confirmingLeave by rememberSaveable { mutableStateOf(false) }
    var chance by rememberSaveable { mutableStateOf<Chance?>(null) }
    val isOver = settings.autoEnd && state.isOver
    val haptic = LocalHapticFeedback.current
    val requestHome = { if (state.inProgress) confirmingLeave = true else onHome() }
    val requestReset = { if (settings.confirmReset) confirmingReset = true else viewModel.resetGame() }

    // System back is the Home button, never a gesture that drops the duel (SPEC §4).
    BackHandler { if (chance != null) chance = null else requestHome() }

    LaunchedEffect(state.lifePoints) { if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    LaunchedEffect(isOver) {
        if (!isOver) return@LaunchedEffect
        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        view.announceForAccessibility(
            view.context.getString(R.string.player_wins, if (state.lifePoints.one > 0) 1 else 2),
        )
    }

    // The power-on intro (SPEC §4): sweep down the matrix (0), totals' lamp test (1), controls in (2), still (3).
    val reduceMotion = reduceMotion()
    var introStep by rememberSaveable { mutableIntStateOf(if (reduceMotion) 3 else 0) }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (introStep != 0) return@LaunchedEffect
        launch { sweep.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        delay(350); introStep = 1
        delay(450); introStep = 2
        delay(400); introStep = 3
    }
    val controls by animateFloatAsState(if (introStep >= 2) 1f else 0f, tween(400), label = "controls")

    // Dims after a while without touches or changes (SPEC §5); the first tap only wakes it.
    var touches by remember { mutableIntStateOf(0) }
    var dimmed by remember { mutableStateOf(false) }
    val talkBack = remember { (view.context.getSystemService(AccessibilityManager::class.java)).isTouchExplorationEnabled }
    LaunchedEffect(state.lifePoints) { touches++ }
    LaunchedEffect(touches, settings.dimAfter) {
        dimmed = false
        // TalkBack users can't see the board anyway, and a dim layer would swallow their first gesture.
        if (settings.dimAfter == 0 || talkBack) return@LaunchedEffect
        delay(settings.dimAfter * 1_000L)
        dimmed = true
    }
    val dim by animateFloatAsState(if (dimmed) 0.85f else 0f, tween(if (dimmed) 1200 else 300), label = "dim")

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) { awaitPointerEvent(PointerEventPass.Initial); touches++ }
            }
        },
    ) {
        LedMatrix()
        if (introStep < 3) LedMatrix(unlit = false) { sweep.value }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val isLandscape = maxWidth > maxHeight
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    state, isOver,
                    Modifier.graphicsLayer { alpha = controls; translationY = (1 - controls) * -12.dp.toPx() },
                    onHome = requestHome,
                    onReset = { if (isOver) viewModel.resetGame() else requestReset() },
                    onTimer = { if (state.timerEndsAt != null) viewModel.pauseTimer() else viewModel.resumeTimer() },
                    onChance = { chance = it },
                )
                // Portrait: Player 2 on top, facing Player 1. Landscape: Player 1 on the left (SPEC §4).
                // Each card gets exactly half: opening one player's history never moves the other's buttons.
                val players = if (isLandscape) listOf(Player.One, Player.Two) else listOf(Player.Two, Player.One)
                val cards: @Composable (Modifier) -> Unit = { m ->
                    players.forEach { player ->
                        GameCard(
                            player = player,
                            lifePoints = state.lifePoints[player],
                            history = state.history.getValue(player),
                            result = if (!isOver) null else state.lifePoints[player] != 0,
                            resets = resets,
                            introStep = introStep,
                            inverted = player == Player.Two,
                            isLandscape = isLandscape,
                            onChange = { viewModel.changeBy(player, it) },
                            onHalve = { viewModel.halve(player) },
                            onUndo = { viewModel.undo(player) },
                            modifier = m,
                        )
                    }
                }
                if (isLandscape) Row(Modifier.weight(1f)) { cards(Modifier.weight(1f)) }
                else Column(Modifier.weight(1f)) { cards(Modifier.weight(1f)) }
            }
        }
        ChanceOverlay(chance) { chance = null }
        if (dim > 0f) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)).clearAndSetSemantics {}
                    .clickable(remember { MutableInteractionSource() }, null) { touches++ },
            )
        }
    }

    if (confirmingLeave) {
        AlertDialog(
            onDismissRequest = { confirmingLeave = false },
            icon = { Icon(Icons.Rounded.Home, null) },
            title = { Text(stringResource(R.string.leave_title)) },
            text = { Text(stringResource(R.string.leave_text)) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { confirmingLeave = false; onHome() }) { Text(stringResource(R.string.action_resume_later)) }
                    TextButton(onClick = { confirmingLeave = false; viewModel.resetGame(); onHome() }) {
                        Text(stringResource(R.string.action_end_duel), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { confirmingLeave = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        )
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            icon = { Icon(Icons.Rounded.RestartAlt, null) },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmingReset = false; viewModel.resetGame() }) {
                    Text(stringResource(R.string.action_reset_game), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Home alone at the left; in the centre the timer (only when active), reset ("New duel" once over), die and coin. */
@Composable
private fun TopBar(
    state: GameState,
    isOver: Boolean,
    modifier: Modifier,
    onHome: () -> Unit,
    onReset: () -> Unit,
    onTimer: () -> Unit,
    onChance: (Chance) -> Unit,
) {
    // Past the rounded corner and the camera cutout's side.
    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        FilledTonalIconButton(onClick = onHome, Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Rounded.Home, stringResource(R.string.tab_home))
        }
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(state.hasTimer, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                val running = state.timerEndsAt != null
                val label = stringResource(if (running) R.string.timer_pause else R.string.timer_resume)
                FilledTonalButton(onClick = onTimer, Modifier.semantics { contentDescription = label }) {
                    Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    TimerReadout(state, 18.sp)
                }
            }
            if (isOver) {
                Button(onClick = onReset) {
                    Icon(Icons.Rounded.RestartAlt, null)
                    Text(stringResource(R.string.action_new_duel), Modifier.padding(start = 6.dp))
                }
            } else {
                FilledTonalIconButton(onClick = onReset) {
                    Icon(Icons.Rounded.RestartAlt, stringResource(R.string.action_reset_game))
                }
            }
            ChanceButtons(onChance)
        }
    }
}
