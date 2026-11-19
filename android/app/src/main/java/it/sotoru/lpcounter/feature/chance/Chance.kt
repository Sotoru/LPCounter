package it.sotoru.lpcounter.feature.chance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.ui.led.DotMatrixText
import it.sotoru.lpcounter.ui.led.LedText
import it.sotoru.lpcounter.ui.led.LocalLed
import it.sotoru.lpcounter.ui.led.dim
import it.sotoru.lpcounter.ui.led.drawDots
import it.sotoru.lpcounter.ui.led.lit
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.reduceMotion
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/** Dice and coin, as the rules allow for card effects (SPEC §4). */
enum class Chance { Die, Coin }

/** The two buttons that open the panel, for the game's top bar or the Home. */
@Composable
fun ChanceButtons(onOpen: (Chance) -> Unit) {
    FilledTonalIconButton(onClick = { onOpen(Chance.Die) }) {
        Icon(Icons.Rounded.Casino, stringResource(R.string.chance_die))
    }
    FilledTonalIconButton(onClick = { onOpen(Chance.Coin) }) {
        Icon(Icons.Rounded.Contrast, stringResource(R.string.chance_coin))
    }
}

/** One throw: the faces it passes through, the last being the fair result. */
private class Flip(val faces: List<String>) {
    val turns get() = faces.size - 1
    val result get() = faces.last()
}

private fun flip(kind: Chance, heads: String, tails: String): Flip = when (kind) {
    Chance.Coin -> {
        val result = if (Random.nextBoolean()) heads else tails
        val other = if (result == heads) tails else heads
        // Heads and tails alternate, ending on the result.
        Flip((0..14).map { if ((it - 14) % 2 == 0) result else other })
    }
    Chance.Die -> {
        // Random faces, never the same twice in a row, ending on the result.
        val faces = mutableListOf(Random.nextInt(1, 7))
        while (faces.size <= 12) faces.add(0, generateSequence { Random.nextInt(1, 7) }.first { it != faces[0] })
        Flip(faces.map(Int::toString))
    }
}

/** Shown while [chance] is set: the coin flips and the die tumbles ~2.5 s, slowing, then lands. Tap outside closes. */
@Composable
fun ChanceOverlay(chance: Chance?, onClose: () -> Unit) {
    AnimatedVisibility(chance != null, enter = fadeIn(), exit = fadeOut()) {
        // Kept while fading out, when chance is already null.
        var kind by remember { mutableStateOf(Chance.Die) }
        if (chance != null) kind = chance
        Panel(kind, onClose)
    }
}

@Composable
private fun Panel(kind: Chance, onClose: () -> Unit) {
    val heads = stringResource(R.string.chance_heads)
    val tails = stringResource(R.string.chance_tails)
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val reduceMotion = reduceMotion()
    val look = LocalLed.current
    var throws by remember { mutableIntStateOf(0) }
    var current by remember { mutableStateOf(Flip(listOf(""))) }
    var progress by remember { mutableFloatStateOf(0f) }
    var settled by remember { mutableStateOf(false) }

    LaunchedEffect(throws, kind) {
        settled = false
        current = flip(kind, heads, tails)
        progress = 0f
        if (!reduceMotion) {
            val duration = if (kind == Chance.Coin) 2.4f else 2.8f
            val start = System.nanoTime()
            do {
                awaitFrame()
                val t = min(1f, (System.nanoTime() - start) / 1e9f / duration)
                // Ease out: fast at first, slowing to a stop.
                progress = 1 - (1 - t).pow(3)
            } while (t < 1f)
        }
        progress = 1f
        settled = true
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        view.announceForAccessibility(current.result)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
        )
        Column(
            Modifier
                .background(Led.background, RoundedCornerShape(28.dp))
                .border(BorderStroke(1.5.dp, look.lit.copy(alpha = 0.3f)), RoundedCornerShape(28.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LedText(stringResource(if (kind == Chance.Die) R.string.chance_die else R.string.chance_coin),
                MaterialTheme.typography.titleSmall, color = look.dim, glow = false)
            Face(current, kind, progress, settled)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) }
                Button(onClick = { throws++ }, enabled = settled) {
                    Icon(Icons.Rounded.Refresh, null)
                    Text(stringResource(R.string.chance_again), Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** The coin: its word squashed edge-on at every half-turn. The die: a die that spins, rolls and changes face. */
@Composable
private fun Face(flip: Flip, kind: Chance, progress: Float, settled: Boolean) {
    val look = LocalLed.current
    val turn = progress * flip.turns
    val index = min(flip.turns, Math.round(turn))
    val squash = max(0.05f, abs(cos(turn * PI.toFloat())))
    val color = if (settled) look.lit else look.dim
    val label = if (settled) flip.result else ""
    if (kind == Chance.Die) {
        val even = index % 2 == 0
        DieFace(
            flip.faces[index].toIntOrNull() ?: 1, color,
            Modifier.padding(24.dp).size(140.dp)
                .semantics { contentDescription = label }
                .scale(if (even) squash else 1f, if (even) 1f else squash)
                .rotate((1 - progress) * 720),
        )
    } else {
        // Both words laid out, the wider one holding the size, so the panel never changes width.
        Box(Modifier.padding(4.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            flip.faces.toSet().forEach { DotMatrixText(it, Modifier.alpha(0f), maxScale = 2) }
            DotMatrixText(flip.faces[index], Modifier.scale(1f, squash), color = color, maxScale = 2)
        }
    }
}

/** One face of a die: a rounded body and its pips, each pip a small cluster of matrix dots. */
@Composable
private fun DieFace(value: Int, color: Color, modifier: Modifier) {
    val look = LocalLed.current
    Canvas(modifier) {
        val side = min(size.width, size.height)
        val inset = 3.dp.toPx()
        val body = Size(side - inset * 2, side - inset * 2)
        val topLeft = Offset((size.width - side) / 2 + inset, (size.height - side) / 2 + inset)
        val radius = CornerRadius(side * 0.24f)
        drawRoundRect(look.glow(color).copy(alpha = 0.08f), topLeft, body, radius)
        drawRoundRect(color, topLeft, body, radius, style = Stroke(4.dp.toPx()))
        val step = (side * 0.6f) / 2
        val start = Offset(topLeft.x - inset + side * 0.2f, topLeft.y - inset + side * 0.2f)
        val dot = side * 0.034f / Led.DOT
        val gap = side * 0.034f * 1.3f
        val points = PIPS.getValue(value).flatMap { (c, r) ->
            val cx = start.x + step * c
            val cy = start.y + step * r
            (-1..1).flatMap { dy ->
                (-1..1).map { dx ->
                    // Corners pulled in, so the cluster is round rather than square.
                    val k = if (abs(dx) + abs(dy) == 2) 0.72f else 1f
                    Offset(cx + dx * gap * k, cy + dy * gap * k)
                }
            }
        }
        drawDots(points, dot, look.pixels, look.glow(color).copy(alpha = 0.4f), scale = 2f)
        drawDots(points, dot, look.pixels, color)
    }
}

/** Pip positions on a 3×3 layout, as on a real die. */
private val PIPS = mapOf(
    1 to listOf(1 to 1),
    2 to listOf(0 to 0, 2 to 2),
    3 to listOf(0 to 0, 1 to 1, 2 to 2),
    4 to listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2),
    5 to listOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2),
    6 to listOf(0 to 0, 2 to 0, 0 to 1, 2 to 1, 0 to 2, 2 to 2),
)
