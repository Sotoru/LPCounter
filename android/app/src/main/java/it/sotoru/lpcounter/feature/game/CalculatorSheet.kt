@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package it.sotoru.lpcounter.feature.game

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.sotoru.lpcounter.R
import kotlinx.coroutines.launch

private sealed interface Key {
    data class Digit(val value: Int) : Key
    data object Toggle : Key
    data object Submit : Key
}

private val Keys: List<Key> =
    (1..9).map(Key::Digit) + Key.Toggle + Key.Digit(0) + Key.Submit

private val CalculatorSaver = listSaver<CalculatorState, Int>(
    save = { listOf(it.input, it.operation.ordinal) },
    restore = { CalculatorState(it[0], Operation.entries[it[1]]) },
)

/**
 * Bottom sheet hosting the calculator. [inverted] rotates the content for the player
 * sitting on the opposite side of the device (portrait only).
 */
@Composable
fun CalculatorSheet(
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box(
            Modifier
                .fillMaxWidth()
                .rotate(if (inverted && !isLandscape) 180f else 0f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Calculator(
                lifePoints = lifePoints,
                isLandscape = isLandscape,
                onClose = close,
                onSubmit = { newLp ->
                    // The sheet only closes when the total actually changed, as on iOS.
                    if (newLp != lifePoints) {
                        onSubmit(newLp)
                        close()
                    }
                },
            )
        }
    }
}

@Composable
private fun Calculator(lifePoints: Int, isLandscape: Boolean, onClose: () -> Unit, onSubmit: (Int) -> Unit) {
    var state by rememberSaveable(stateSaver = CalculatorSaver) { mutableStateOf(CalculatorState()) }
    // The landscape sheet is only ~640dp wide, so its 4-column keypad needs the smaller keys.
    val button = if (isLandscape) IconButtonDefaults.mediumContainerSize() else IconButtonDefaults.largeContainerSize()
    val gap = if (isLandscape) 12.dp else 16.dp

    val keypad = @Composable {
        Keypad(
            columns = if (isLandscape) 4 else 3,
            button = button,
            gap = gap,
            state = state,
            onChange = { state = it },
            onSubmit = { onSubmit(state.applyTo(lifePoints)); state = state.clear() },
        )
    }
    // Same three columns as the keypad, so close/clear sit exactly above the keys' edges.
    val display = @Composable {
        Display(
            width = button.width * 3 + gap * 2,
            lifePoints = lifePoints,
            state = state,
            button = button,
            gap = gap,
            onClose = onClose,
            onClear = { state = state.clear() },
        )
    }

    if (isLandscape) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) { keypad(); display() }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            display(); keypad()
        }
    }
}

@Composable
private fun Display(
    width: Dp,
    lifePoints: Int,
    state: CalculatorState,
    button: DpSize,
    gap: Dp,
    onClose: () -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.width(button.width), contentAlignment = Alignment.Center) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) }
            }
        }
        Text(
            lifePoints.toString(),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displayLargeEmphasized,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 32.sp, maxFontSize = 88.sp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(button), contentAlignment = Alignment.Center) {
                Text(
                    if (state.operation == Operation.Increase) "+" else "−",
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Crossfade(state.input != 0, Modifier.size(button), animationSpec = tween(150), label = "value") { hasInput ->
                Box(Modifier.size(button), contentAlignment = Alignment.Center) {
                    Text(
                        if (hasInput) state.value.toString() else "–",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        color = if (hasInput) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 32.sp),
                    )
                }
            }
            FilledTonalIconButton(onClick = onClear, modifier = Modifier.size(button), shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.action_clear_input))
            }
        }
    }
}

@Composable
private fun Keypad(
    columns: Int,
    button: DpSize,
    gap: Dp,
    state: CalculatorState,
    onChange: (CalculatorState) -> Unit,
    onSubmit: () -> Unit,
) {
    val toggleDescription = stringResource(R.string.action_toggle_operation)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Keys.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { key ->
                    when (key) {
                        is Key.Digit -> FilledTonalIconButton(
                            onClick = { onChange(state.append(key.value)) },
                            modifier = Modifier.size(button),
                            shapes = IconButtonDefaults.shapes(),
                        ) { Text(key.value.toString(), style = MaterialTheme.typography.headlineMediumEmphasized) }

                        Key.Toggle -> FilledTonalIconButton(
                            onClick = { onChange(state.toggleOperation()) },
                            modifier = Modifier.size(button).semantics {
                                contentDescription = toggleDescription
                            },
                            shapes = IconButtonDefaults.shapes(),
                        ) { Text("±", style = MaterialTheme.typography.headlineMediumEmphasized) }

                        Key.Submit -> FilledIconButton(
                            onClick = onSubmit,
                            modifier = Modifier.size(button),
                            shapes = IconButtonDefaults.shapes(),
                        ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.action_submit)) }
                    }
                }
            }
        }
    }
}
