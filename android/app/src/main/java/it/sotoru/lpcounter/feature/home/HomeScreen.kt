package it.sotoru.lpcounter.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.feature.chance.Chance
import it.sotoru.lpcounter.feature.chance.ChanceButtons
import it.sotoru.lpcounter.feature.chance.ChanceOverlay
import it.sotoru.lpcounter.feature.game.GameViewModel
import it.sotoru.lpcounter.feature.timer.TimerMenu
import it.sotoru.lpcounter.feature.timer.TimerReadout
import it.sotoru.lpcounter.feature.timer.timerLabel
import it.sotoru.lpcounter.ui.led.DotMatrixText
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedMatrix
import it.sotoru.lpcounter.ui.led.LedText
import it.sotoru.lpcounter.ui.led.LocalLed
import it.sotoru.lpcounter.ui.led.SegmentReadout
import it.sotoru.lpcounter.ui.led.dim
import it.sotoru.lpcounter.ui.led.lit

/** The series' catchphrase, kept in English in every language; "DUEL" is the button that ends it. */
private val Catchphrase = listOf("IT'S", "TIME", "TO")

/** The root (SPEC §9): settings on top; the call to duel centred, a duel in progress under it, and Play; die, coin and timer below. */
@Composable
fun HomeScreen(state: GameState, viewModel: GameViewModel, onPlay: () -> Unit, onSettings: () -> Unit) {
    var chance by rememberSaveable { mutableStateOf<Chance?>(null) }
    Box(Modifier.fillMaxSize()) {
        LedMatrix()
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                FilledTonalIconButton(onClick = onSettings) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.settings_title))
                }
            }
            Hero(state, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) { viewModel.startDuel(); onPlay() }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChanceButtons { chance = it }
                Spacer(Modifier.weight(1f))
                TimerControls(state, viewModel)
            }
        }
        ChanceOverlay(chance) { chance = null }
    }
}

@Composable
private fun Hero(state: GameState, modifier: Modifier, onPlay: () -> Unit) {
    val look = LocalLed.current
    val callToDuel = stringResource(R.string.home_catchphrase)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
    ) {
        Column(
            Modifier.clearAndSetSemantics { contentDescription = callToDuel; heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Catchphrase.forEach { DotMatrixText(it, maxScale = 2) }
        }
        AnimatedVisibility(state.inProgress, enter = fadeIn(), exit = fadeOut()) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Player.entries.forEach { player ->
                    val lp = state.lifePoints[player]
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LedText(stringResource(R.string.player_name, player.number), MaterialTheme.typography.labelMedium,
                            color = look.dim, glow = false)
                        SegmentReadout(lp.toString(), 4, 30.sp, color = if (lp == 0) Led.red else look.lit)
                    }
                }
            }
        }
        Button(onClick = onPlay, Modifier.widthIn(max = 300.dp).fillMaxWidth().heightIn(min = 64.dp)) {
            Text(
                if (state.inProgress) stringResource(R.string.action_resume) else "DUEL",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** The round timer and, when there is one, its remove button (SPEC §9). */
@Composable
private fun TimerControls(state: GameState, viewModel: GameViewModel) {
    var menu by rememberSaveable { mutableStateOf(false) }
    val off = state.timerMinutes == 0 && !state.hasTimer
    Box {
        if (off) {
            FilledTonalIconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.TimerOff, stringResource(R.string.settings_timer), Modifier.alpha(0.6f))
            }
        } else {
            FilledTonalButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.Timer, null)
                Spacer(Modifier.width(8.dp))
                if (state.hasTimer) TimerReadout(state, 14.sp) else Text(timerLabel(state.timerMinutes))
            }
        }
        TimerMenu(menu, state.timerMinutes, onDismiss = { menu = false }, onSelect = viewModel::setTimerMinutes)
    }
    AnimatedVisibility(state.hasTimer, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        FilledTonalIconButton(onClick = viewModel::removeTimer) {
            Icon(Icons.Rounded.Close, stringResource(R.string.timer_remove))
        }
    }
}
