package it.sotoru.lpcounter.feature.game

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.data.Settings
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameScreen(state: GameState, settings: Settings, viewModel: GameViewModel, onHome: () -> Unit) {
    // Equivalent of UIApplication.isIdleTimerDisabled: the table keeps the screen awake.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    BackHandler(onBack = onHome)

    // On phones the duel is always landscape (SPEC §4); tablets lay out by window shape. The Home is
    // unlocked again when the game leaves composition.
    val activity = LocalActivity.current
    val isPhone = LocalConfiguration.current.smallestScreenWidthDp < 600
    DisposableEffect(activity, isPhone) {
        if (isPhone) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if (isPhone) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    val isOver = settings.autoEnd && state.isOver
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.lifePoints) { if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    LaunchedEffect(isOver) { if (isOver && settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.Confirm) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            // Portrait: Player 2 on top, facing Player 1. Landscape: Player 1 on the left (SPEC §4).
            val cards: @Composable (Modifier) -> Unit = { cardModifier ->
                (if (isLandscape) listOf(Player.One, Player.Two) else listOf(Player.Two, Player.One)).forEach { player ->
                    GameCard(
                        lifePoints = state.lifePoints[player],
                        startingLifePoints = state.startingLifePoints,
                        history = state.history.getValue(player),
                        result = if (!isOver) null else state.lifePoints[player] != 0,
                        inverted = player == Player.Two,
                        isLandscape = isLandscape,
                        onSetLifePoints = { viewModel.changeBy(player, it - state.lifePoints[player]) },
                        onHalve = { viewModel.halve(player) },
                        onUndo = { viewModel.undo(player) },
                        onReset = { viewModel.resetPlayer(player) },
                        modifier = cardModifier,
                    )
                }
            }
            if (isLandscape) {
                Row(Modifier.fillMaxSize().safeDrawingPadding()) { cards(Modifier.weight(1f)) }
            } else {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) { cards(Modifier.weight(1f)) }
            }

            // Along the seam between the cards: a row in portrait, a column in landscape. Icons stay upright.
            val controls: @Composable () -> Unit = {
                FilledTonalIconButton(onClick = onHome) {
                    Icon(Icons.Rounded.Home, stringResource(R.string.tab_home))
                }
                state.timerEndsAt?.let { TimerText(it) }
                if (isOver) {
                    FilledIconButton(onClick = viewModel::resetGame) {
                        Icon(Icons.Rounded.RestartAlt, stringResource(R.string.action_new_duel))
                    }
                } else {
                    FilledTonalIconButton(onClick = { if (settings.confirmReset) confirmingReset = true else viewModel.resetGame() }) {
                        Icon(Icons.Rounded.RestartAlt, stringResource(R.string.action_reset_game))
                    }
                }
            }
            Box(Modifier.align(Alignment.Center)) {
                if (isLandscape) {
                    VerticalFloatingToolbar(expanded = true) { controls() }
                } else {
                    HorizontalFloatingToolbar(expanded = true) { controls() }
                }
            }
        }
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            icon = { Icon(Icons.Rounded.RestartAlt, null) },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmingReset = false; viewModel.resetGame() }) {
                    Text(stringResource(R.string.action_reset_game))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Remaining round time, `m:ss`; stays at 0:00 once it runs out (SPEC §4). */
@Composable
private fun TimerText(endsAt: Long) {
    val left by produceState(endsAt - System.currentTimeMillis(), endsAt) {
        while (true) {
            value = endsAt - System.currentTimeMillis()
            delay(1_000 - (value % 1_000).coerceAtLeast(0))
        }
    }
    val seconds = ((left + 999) / 1_000).coerceAtLeast(0)
    Text(
        "%d:%02d".format(seconds / 60, seconds % 60),
        Modifier.padding(horizontal = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        color = if (seconds == 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}
