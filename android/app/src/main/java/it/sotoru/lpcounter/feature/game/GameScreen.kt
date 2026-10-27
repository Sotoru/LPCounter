package it.sotoru.lpcounter.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.Player

@Composable
fun GameRoute(viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Equivalent of UIApplication.isIdleTimerDisabled: the table keeps the screen awake.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    state?.let { GameScreen(it, viewModel) }
}

@Composable
private fun GameScreen(state: GameState, viewModel: GameViewModel) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            // Player 2 first: top in portrait, left in landscape.
            val cards: @Composable (Modifier) -> Unit = { cardModifier ->
                listOf(Player.Two, Player.One).forEach { player ->
                    GameCard(
                        lifePoints = state.lifePoints[player],
                        history = state.history.getValue(player),
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

            Box(Modifier.align(Alignment.Center)) {
                FloatingActionButton(
                    onClick = viewModel::resetGame,
                    modifier = Modifier.rotate(if (isLandscape) 270f else 0f),
                ) { Icon(Icons.Rounded.RestartAlt, stringResource(R.string.action_reset_game)) }
            }
        }
    }
}
