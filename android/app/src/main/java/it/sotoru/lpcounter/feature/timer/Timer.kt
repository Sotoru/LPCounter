package it.sotoru.lpcounter.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.TIMER_PRESETS
import it.sotoru.lpcounter.data.TIMER_RANGE
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LocalLed
import it.sotoru.lpcounter.ui.led.SegmentReadout
import it.sotoru.lpcounter.ui.led.dim
import it.sotoru.lpcounter.ui.led.lit
import kotlinx.coroutines.delay

/** `m:ss` (or `h:mm:ss`); [millis] rounded up so a running timer reads 0:00 only once it is up. */
fun formatTimer(millis: Long): String {
    val s = ((millis + 999) / 1_000).coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** The time left in seven segments: live while running (0:00 in red once up), frozen and dimmed while paused. */
@Composable
fun TimerReadout(state: GameState, size: TextUnit = 18.sp) {
    val look = LocalLed.current
    val endsAt = state.timerEndsAt
    val left by produceState(endsAt?.let { it - System.currentTimeMillis() } ?: 0L, endsAt) {
        endsAt ?: return@produceState
        while (true) {
            value = endsAt - System.currentTimeMillis()
            delay(1_000 - (value % 1_000).coerceAtLeast(0))
        }
    }
    when {
        endsAt != null -> SegmentReadout(formatTimer(left), 4, size, color = if (left <= 0) Led.red else look.lit)
        state.timerPausedRemaining != null -> SegmentReadout(formatTimer(state.timerPausedRemaining), 4, size, color = look.dim)
    }
}

/** "Off" or "50 min". */
@Composable
fun timerLabel(minutes: Int) = if (minutes == 0) stringResource(R.string.timer_off) else stringResource(R.string.timer_minutes, minutes)

/** Off, the presets, the custom length if any, and Custom…: the check follows the choice (SPEC §9). */
@Composable
fun TimerMenu(expanded: Boolean, minutes: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    DropdownMenu(expanded, onDismiss) {
        val choices = listOf(0) + TIMER_PRESETS + listOfNotNull(minutes.takeIf { it != 0 && it !in TIMER_PRESETS })
        choices.forEach { option ->
            DropdownMenuItem(
                text = { Text(timerLabel(option)) },
                onClick = { onSelect(option); onDismiss() },
                leadingIcon = { Icon(if (option == 0) Icons.Rounded.TimerOff else Icons.Rounded.Timer, null) },
                trailingIcon = { if (option == minutes) Icon(Icons.Rounded.Check, null) },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.timer_custom)) },
            onClick = { editing = true; onDismiss() },
            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
        )
    }
    if (editing) TimerEditor(minutes, onDismiss = { editing = false }, onDone = { onSelect(it); editing = false })
}

/** Any round length, 1–180 min in steps of 5. */
@Composable
private fun TimerEditor(minutes: Int, onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    var value by rememberSaveable { mutableIntStateOf(if (minutes == 0) TIMER_PRESETS.last() else minutes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Timer, null) },
        title = { Text(stringResource(R.string.timer_title)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalIconButton(
                    onClick = { value = (value - 5).coerceIn(TIMER_RANGE) },
                    enabled = value > TIMER_RANGE.first,
                ) { Icon(Icons.Rounded.Remove, stringResource(R.string.timer_shorter)) }
                Text(stringResource(R.string.timer_minutes, value), Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                FilledTonalIconButton(
                    onClick = { value = (value + 5).coerceIn(TIMER_RANGE) },
                    enabled = value < TIMER_RANGE.last,
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.timer_longer)) }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(value) }) { Text(stringResource(R.string.action_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
