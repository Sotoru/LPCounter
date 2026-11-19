@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package it.sotoru.lpcounter.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.ui.led.DotMatrixNumber
import it.sotoru.lpcounter.ui.led.DotMatrixText
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedMatrix
import it.sotoru.lpcounter.ui.led.LedText
import it.sotoru.lpcounter.ui.led.LocalLed
import it.sotoru.lpcounter.ui.led.dim
import it.sotoru.lpcounter.ui.led.lit
import kotlinx.coroutines.launch

/** Spacing on the matrix's pitch (5 dp), so keys and gaps line up with the dots behind. */
private val Gap = 15.dp
private val KeyHeight = 55.dp
private val ColumnWidth = 420.dp

private val CalculatorSaver = listSaver<CalculatorState, Int>(
    save = { listOf(it.input, it.operation.ordinal) },
    restore = { CalculatorState(it[0], Operation.entries[it[1]]) },
)

/**
 * Bottom sheet hosting the keypad (SPEC §3), on the app's dot matrix. [inverted] turns the content for the
 * player across the table (portrait only).
 */
@Composable
fun CalculatorSheet(
    player: Player,
    lifePoints: Int,
    isLandscape: Boolean,
    inverted: Boolean,
    onSubmit: (newLifePoints: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Led.background, dragHandle = null) {
        Box(Modifier.fillMaxWidth().rotate(if (inverted && !isLandscape) 180f else 0f)) {
            LedMatrix()
            Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    IconButton(onClick = close) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) }
                }
                Calculator(player, lifePoints, isLandscape) { newLp ->
                    // The sheet only closes when the total actually changed.
                    if (newLp != lifePoints) {
                        onSubmit(newLp)
                        close()
                    }
                }
            }
        }
    }
}

@Composable
private fun Calculator(player: Player, lifePoints: Int, isLandscape: Boolean, onSubmit: (Int) -> Unit) {
    var state by rememberSaveable(stateSaver = CalculatorSaver) { mutableStateOf(CalculatorState()) }
    val haptic = LocalHapticFeedback.current
    val change: (CalculatorState) -> Unit = {
        if (it != state) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        state = it
    }
    val submit = { onSubmit(state.applyTo(lifePoints)); state = state.clear() }

    if (isLandscape) {
        // Two equal columns; the switch sits on the keypad's last row.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap * 2, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).widthIn(max = ColumnWidth), verticalArrangement = Arrangement.spacedBy(Gap)) {
                Display(player, lifePoints, state)
                OperationSwitch(state, change)
            }
            Keypad(4, state, change, submit, Modifier.weight(1f).widthIn(max = ColumnWidth))
        }
    } else {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Gap)) {
            Display(player, lifePoints, state)
            Column(Modifier.widthIn(max = ColumnWidth), verticalArrangement = Arrangement.spacedBy(Gap)) {
                OperationSwitch(state, change)
                Keypad(3, state, change, submit)
            }
        }
    }
}

/** On the matrix, like the board: the total now, the change typed, and what it leads to. */
@Composable
private fun Display(player: Player, lifePoints: Int, state: CalculatorState) {
    val look = LocalLed.current
    val adding = state.operation == Operation.Increase
    val opColor = if (adding) Led.green else Led.red
    val result = state.applyTo(lifePoints)
    val name = stringResource(R.string.player_name, player.number)
    val total = stringResource(R.string.calc_total, lifePoints)
    val label = if (state.input == 0) "$name, $total" else "$name, $total, " +
        stringResource(if (adding) R.string.calc_add else R.string.calc_subtract, state.value) + ", " +
        stringResource(R.string.calc_result, result)
    Column(
        Modifier.fillMaxWidth().padding(vertical = Gap).clearAndSetSemantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        LedText(name, MaterialTheme.typography.titleSmall, color = look.dim, glow = false)
        DotMatrixNumber(lifePoints, maxScale = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
            DotMatrixText(if (adding) "+" else "-", color = opColor)
            DotMatrixText(if (state.input == 0) "    " else state.value.toString(), color = opColor)
            DotMatrixText("=", color = look.dim, glow = false)
            DotMatrixText(if (state.input == 0) "    " else result.toString(), color = if (result == 0) Led.red else look.lit)
        }
    }
}

/** Two big halves, the active one filled in its colour: one tap on the side you want. */
@Composable
private fun OperationSwitch(state: CalculatorState, onChange: (CalculatorState) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
        Side(R.string.action_subtract, Icons.Rounded.Remove, state.operation == Operation.Decrease, Led.red, Modifier.weight(1f)) {
            if (state.operation != Operation.Decrease) onChange(state.toggleOperation())
        }
        Side(R.string.action_add, Icons.Rounded.Add, state.operation == Operation.Increase, Led.green, Modifier.weight(1f)) {
            if (state.operation != Operation.Increase) onChange(state.toggleOperation())
        }
    }
}

@Composable
private fun Side(title: Int, icon: ImageVector, on: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Icon(icon, null)
        Text(stringResource(title), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
    }
    val m = modifier.heightIn(min = KeyHeight).semantics { selected = on }
    val padding = PaddingValues(horizontal = 8.dp)
    if (on) {
        Button(onClick, m, colors = ButtonDefaults.buttonColors(color, Led.background), contentPadding = padding) { content() }
    } else {
        OutlinedButton(onClick, m, colors = ButtonDefaults.outlinedButtonColors(contentColor = color), contentPadding = padding) { content() }
    }
}

private sealed interface Key {
    data class Digit(val value: Int) : Key
    data object Clear : Key
    data object Submit : Key
}

private val Keys: List<Key> = (1..9).map(Key::Digit) + Key.Clear + Key.Digit(0) + Key.Submit

@Composable
private fun Keypad(
    columns: Int,
    state: CalculatorState,
    onChange: (CalculatorState) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opColor = if (state.operation == Operation.Increase) Led.green else Led.red
    val digit = TextStyle(fontFamily = Led.segmentFont, fontSize = 26.sp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Gap)) {
        Keys.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                row.forEach { key ->
                    val m = Modifier.weight(1f).heightIn(min = KeyHeight)
                    when (key) {
                        is Key.Digit -> FilledTonalButton({ onChange(state.append(key.value)) }, m) {
                            Text(key.value.toString(), style = digit)
                        }
                        Key.Clear -> FilledTonalButton({ onChange(state.clear()) }, m, enabled = state.input != 0) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.action_clear_input))
                        }
                        Key.Submit -> Button(onSubmit, m, enabled = state.input != 0,
                            colors = ButtonDefaults.buttonColors(opColor, Led.background)) {
                            Icon(Icons.Rounded.Check, stringResource(R.string.action_submit))
                        }
                    }
                }
                // Keeps the last row's keys the same width as the others.
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}
