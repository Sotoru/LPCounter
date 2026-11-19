package it.sotoru.lpcounter.feature.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.ui.led.DotMatrixNumber
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedText
import it.sotoru.lpcounter.ui.led.LocalLed
import it.sotoru.lpcounter.ui.led.dim
import it.sotoru.lpcounter.ui.led.lit
import it.sotoru.lpcounter.ui.reduceMotion
import kotlin.math.abs

/** Height of the area above the buttons: a total at full size, or the history in its place. */
private val DisplayHeight = 140.dp

/**
 * Right +100, left −100, up +1000, down −1000, along the swipe's main axis (SPEC §4). The drag is read in the
 * card's own space, so Player 2's card, turned 180° in portrait, reads from its player's side.
 */
fun swipeDelta(dx: Float, dy: Float): Int =
    if (abs(dx) > abs(dy)) (if (dx > 0) 100 else -100) else (if (dy < 0) 1000 else -1000)

/** One player's card (SPEC §4): name, the total or its history in one fixed area, and the buttons below. */
@Composable
fun GameCard(
    player: Player,
    lifePoints: Int,
    history: List<Int>,
    /** null while playing; true = this player won, false = lost (SPEC §4). */
    result: Boolean?,
    resets: Int,
    introStep: Int,
    inverted: Boolean,
    isLandscape: Boolean,
    onChange: (Int) -> Unit,
    onHalve: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = LocalLed.current
    var showsHistory by rememberSaveable { mutableStateOf(false) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var halving by remember { mutableStateOf(false) }

    Column(
        // Players face each other across the device in portrait; in landscape both read upright.
        modifier.padding(8.dp).rotate(if (inverted && !isLandscape) 180f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LedText(
            stringResource(R.string.player_name, player.number),
            MaterialTheme.typography.titleMedium,
            Modifier.alpha(if (introStep >= 2) 1f else 0f).semantics { heading() },
            color = look.lit.copy(alpha = 0.75f),
            glow = false,
        )
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            // One fixed area for the total and the history, so the buttons below stay where they are.
            Box(Modifier.fillMaxWidth().height(DisplayHeight).alpha(if (introStep >= 1) 1f else 0f), contentAlignment = Alignment.Center) {
                AnimatedContent(showsHistory, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { history_ ->
                    if (history_) HistoryList(history)
                    else Total(player, lifePoints, resets, introStep, halving, result == null, { halving = false }, onChange) { showSheet = true }
                }
            }
            Box(Modifier.alpha(if (introStep >= 2) 1f else 0f)) {
                if (result != null) ResultBadge(won = result)
                else Actions(showsHistory, history.isEmpty(), lifePoints, { showsHistory = !showsHistory }, onUndo) {
                    // Halving 1 leaves 1: no change, so no effect waiting for one.
                    if (lifePoints > 1) halving = true
                    onHalve()
                }
            }
        }
    }

    if (showSheet) {
        CalculatorSheet(
            player = player,
            lifePoints = lifePoints,
            isLandscape = isLandscape,
            inverted = inverted,
            onSubmit = { onChange(it - lifePoints) },
            onDismiss = { showSheet = false },
        )
    }
}

/**
 * The total on the grid. Plain changes count to the new value; a game reset runs the lamp test (every dot lit
 * from the bottom, then the new total) and halve dissolves the old total dot by dot into its half (SPEC §4).
 */
@Composable
private fun Total(
    player: Player,
    lifePoints: Int,
    resets: Int,
    introStep: Int,
    halving: Boolean,
    enabled: Boolean,
    onHalved: () -> Unit,
    onChange: (Int) -> Unit,
    onOpen: () -> Unit,
) {
    val reduceMotion = reduceMotion()
    val shown = remember { Animatable(lifePoints.toFloat()) }
    val test = remember { Animatable(0f) }
    val morph = remember { Animatable(1f) }
    var from by remember { mutableIntStateOf(lifePoints) }
    var seenResets by remember { mutableIntStateOf(resets) }

    LaunchedEffect(lifePoints, resets) {
        val reset = resets != seenResets
        seenResets = resets
        val old = shown.value.toInt()
        if (old == lifePoints) return@LaunchedEffect
        when {
            reduceMotion -> shown.snapTo(lifePoints.toFloat())
            reset -> { shown.snapTo(lifePoints.toFloat()); lampTest(test) }
            halving -> {
                from = old
                shown.snapTo(lifePoints.toFloat())
                morph.snapTo(0f)
                morph.animateTo(1f, tween(750, delayMillis = 120, easing = LinearEasing))
            }
            else -> shown.animateTo(lifePoints.toFloat(), spring(stiffness = 300f))
        }
        onHalved()
    }
    // The board's power-on lights the total with the same lamp test.
    // Keyed on reaching step 1, not on the step: the next step must not cancel it halfway, fully lit.
    val poweredOn = introStep >= 1
    LaunchedEffect(poweredOn) { if (poweredOn && introStep == 1 && !reduceMotion) lampTest(test) }

    val lp = shown.value.toInt()
    val name = stringResource(R.string.player_life_points, player.number)
    val add = stringResource(R.string.action_add_1000)
    val subtract = stringResource(R.string.action_subtract_1000)
    val edit = stringResource(R.string.action_open_calculator)
    DotMatrixNumber(
        lp,
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .clickable(enabled = enabled, onClick = onOpen)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var drag = Offset.Zero
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDragEnd = { if (drag.getDistance() > 24.dp.toPx()) onChange(swipeDelta(drag.x, drag.y)) },
                ) { _, amount -> drag += amount }
            }
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = lifePoints.toString()
                if (!enabled) return@clearAndSetSemantics
                onClick(edit) { onOpen(); true }
                // TalkBack's adjust gesture changes by 100, like the swipe.
                progressBarRangeInfo = ProgressBarRangeInfo(lifePoints.toFloat(), 0f..100_000f, steps = 0)
                setProgress { onChange(if (it > lifePoints) 100 else -100); true }
                customActions = listOf(
                    CustomAccessibilityAction(add) { onChange(1000); true },
                    CustomAccessibilityAction(subtract) { onChange(-1000); true },
                )
            },
        color = if (lifePoints == 0) Led.red else LocalLed.current.lit,
        previous = if (morph.value < 1f) from else null,
        morph = morph.value,
        test = test.value,
    )
}

private suspend fun lampTest(test: Animatable<Float, *>) {
    test.snapTo(0f)
    test.animateTo(1f, tween(180, easing = LinearEasing))
    test.animateTo(1f, snap(140))
    test.animateTo(0f, tween(500))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Actions(
    showsHistory: Boolean,
    historyEmpty: Boolean,
    lifePoints: Int,
    onToggle: () -> Unit,
    onUndo: () -> Unit,
    onHalve: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Same place in both states, only the symbol changes.
        CardButton(if (showsHistory) Icons.Rounded.Numbers else Icons.Rounded.History,
            if (showsHistory) R.string.action_show_total else R.string.action_show_history, true, onToggle)
        CardButton(Icons.AutoMirrored.Rounded.Undo, R.string.action_undo, !historyEmpty, onUndo)
        CardButton(DivideIcon, R.string.action_halve, lifePoints != 0, onHalve)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CardButton(icon: ImageVector, description: Int, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(IconButtonDefaults.mediumContainerSize().height),
        shapes = IconButtonDefaults.shapes(),
    ) { Icon(icon, stringResource(description)) }
}

@Composable
private fun ResultBadge(won: Boolean) {
    val pulse by rememberInfiniteTransition(label = "result").animateFloat(
        1f, if (won && !reduceMotion()) 0.55f else 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    LedText(
        stringResource(if (won) R.string.result_winner else R.string.result_defeated),
        MaterialTheme.typography.headlineMedium,
        Modifier.alpha(pulse),
        color = if (won) Led.green else Led.red,
    )
}

@Composable
private fun HistoryList(history: List<Int>) {
    val look = LocalLed.current
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LedText(stringResource(R.string.no_history), MaterialTheme.typography.titleMedium, color = look.dim, glow = false)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Newest first; entries are signed changes, so duplicates are possible and position is the key.
        itemsIndexed(history.asReversed(), key = { index, _ -> history.size - index }) { _, number ->
            LedText(
                "${if (number > 0) "+" else "−"} ${abs(number)}",
                MaterialTheme.typography.titleLarge,
                Modifier.padding(vertical = 6.dp),
                color = if (number > 0) Led.green else Led.red,
            )
        }
    }
}

/** "÷": Material has no divide symbol among the rounded icons. */
private val DivideIcon: ImageVector by lazy {
    ImageVector.Builder("Divide", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                "M5,11h14v2H5z M12,5.5a1.75,1.75 0 1,1 0,3.5a1.75,1.75 0 1,1 0,-3.5z M12,15a1.75,1.75 0 1,1 0,3.5a1.75,1.75 0 1,1 0,-3.5z",
            ).toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black),
        )
    }.build()
}
