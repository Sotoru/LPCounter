package it.sotoru.lpcounter.ui.led

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import it.sotoru.lpcounter.R
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.sin

/** Colour of the display (SPEC §5): the tube, its halo and the unlit dots. Same values as iOS `LEDPalette`. */
enum class LedPalette(val lit: Color, val halo: Color, val unlit: Color, @StringRes val title: Int) {
    Night(Color(0xFFA9C8FF), Color(0xFF3E6BFF), Color(0xFF0C111F), R.string.palette_night),
    Ice(Color(0xFFE8F3FF), Color(0xFF6E9CFF), Color(0xFF111722), R.string.palette_ice),
    Frost(Color(0xFFD6FAFF), Color(0xFF57C7E8), Color(0xFF0E1A1E), R.string.palette_frost),
    Cyan(Color(0xFF5FF5E3), Color(0xFF2BD8C4), Color(0xFF0A1F1D), R.string.palette_cyan),
    Amber(Color(0xFFFFC46B), Color(0xFFFF7A1A), Color(0xFF1E1308), R.string.palette_amber),
}

/** Shape of every dot of the grid, lit or not (SPEC §5): kept far apart so the choice shows on small dots. */
enum class PixelStyle(@StringRes val title: Int) {
    Round(R.string.pixel_round), Diamond(R.string.pixel_diamond), Square(R.string.pixel_square)
}

@Immutable
data class LedLook(val palette: LedPalette = LedPalette.Night, val pixels: PixelStyle = PixelStyle.Round) {
    /** The halo of a lit colour: the palette's deeper one for the main colour, itself for red and green. */
    fun glow(color: Color) = if (color == palette.lit) palette.halo else color
}

object Led {
    /** Pure black: on OLED screens those pixels are off, as on a real display between its dots. */
    val background = Color.Black
    val red = Color(1f, 0.365f, 0.365f)
    val green = Color(0.616f, 1f, 0.541f)
    /** Behind list rows: dark enough to read on, light enough to let the grid show through. */
    val panel = Color.Black.copy(alpha = 0.55f)
    /** Dot diameter as a share of the pitch, the same for unlit and lit dots. */
    const val DOT = 0.72f
    val segmentFont = FontFamily(Font(R.font.dseg7_classic_bold))
}

val LocalLed = compositionLocalOf { LedLook() }
/** Distance between two dots of the screen's grid; widgets use a finer one. */
val LocalLedPitch = compositionLocalOf { 5.dp }

val LedLook.lit get() = palette.lit
/** Secondary text: 0.6 keeps it above 4.5:1 on black. */
val LedLook.dim get() = palette.lit.copy(alpha = 0.6f)

// region Dots

private const val INV_SQRT2 = 0.70710677f

/** Draws one dot of [style] centred on each point; [scale] enlarges them (halos). */
fun DrawScope.drawDots(points: List<Offset>, p: Float, style: PixelStyle, color: Color, scale: Float = 1f) {
    if (points.isEmpty()) return
    val d = p * Led.DOT * scale
    when (style) {
        PixelStyle.Round -> drawPoints(points, PointMode.Points, color, d, StrokeCap.Round)
        PixelStyle.Square -> drawPoints(points, PointMode.Points, color, d * 1.24f, StrokeCap.Square)
        // Square caps in a canvas turned 45°, at points turned back: each dot a diamond, in place.
        PixelStyle.Diamond -> rotate(45f, Offset.Zero) {
            drawPoints(
                points.map { Offset((it.x + it.y) * INV_SQRT2, (it.y - it.x) * INV_SQRT2) },
                PointMode.Points, color, d * 0.877f, StrokeCap.Square,
            )
        }
    }
}

private fun center(origin: Offset, col: Int, row: Int, p: Float) =
    Offset(origin.x + (col + 0.5f) * p, origin.y + (row + 0.5f) * p)

private fun gridPoints(origin: Offset, p: Float, size: Size): List<Offset> {
    val cols = (size.width / p).toInt() + 1
    val rows = (size.height / p).toInt() + 1
    return ArrayList<Offset>(cols * rows).apply {
        for (r in -1..rows) for (c in -1..cols) add(center(origin, c, r, p))
    }
}

/** Where a view sits in the window, so its dots land on the screen's one grid (SPEC §10), turned or not. */
private class GridAnchor {
    var value by mutableStateOf<Pair<Offset, Offset>?>(null)

    fun origin(p: Float): Offset? {
        val (o, s) = value ?: return null
        fun phase(v: Float) = ((v % p) + p) % p
        return Offset(phase(-s.x * o.x), phase(-s.y * o.y))
    }
}

private fun Modifier.anchor(a: GridAnchor) = onGloballyPositioned { c ->
    val o = c.localToWindow(Offset.Zero)
    val u = c.localToWindow(Offset(1f, 1f)) - o
    a.value = o to Offset(sign(u.x), sign(u.y))
}

/** A canvas on the screen's grid: [draw] gets the first cell's offset and the pitch in px. */
@Composable
fun GridCanvas(modifier: Modifier, draw: DrawScope.(origin: Offset, p: Float) -> Unit) {
    val pitch = LocalLedPitch.current
    val anchor = remember { GridAnchor() }
    Spacer(modifier.anchor(anchor).drawBehind {
        val p = pitch.toPx()
        anchor.origin(p)?.let { draw(it, p) }
    })
}

/**
 * The unlit matrix behind a screen. [sweep] (0..1, read while drawing) runs a band of lit dots down it,
 * as a panel switching on; [unlit] false draws the band alone, over a matrix drawn elsewhere.
 */
@Composable
fun LedMatrix(modifier: Modifier = Modifier, unlit: Boolean = true, sweep: (() -> Float?)? = null) {
    val look = LocalLed.current
    val pitch = LocalLedPitch.current
    val anchor = remember { GridAnchor() }
    Spacer(
        modifier.fillMaxSize().anchor(anchor).clearAndSetSemantics {}.drawWithCache {
            val p = pitch.toPx()
            val o = anchor.origin(p)
            val dots = if (unlit && o != null) gridPoints(o, p, size) else emptyList()
            onDrawBehind {
                if (unlit) drawRect(Led.background)
                drawDots(dots, p, look.pixels, look.palette.unlit)
                val t = sweep?.invoke() ?: return@onDrawBehind
                if (o == null) return@onDrawBehind
                val rows = (size.height / p).toInt()
                val centre = (rows + 16) * t - 8
                val band = ArrayList<Offset>()
                for (r in (centre - 4).toInt()..(centre + 4).toInt()) {
                    if (abs(r - centre) >= 4) continue
                    for (c in -1..(size.width / p).toInt()) band += center(o, c, r, p)
                }
                drawDots(band, p, look.pixels, look.palette.halo.copy(alpha = 0.3f), scale = 1.8f)
                drawDots(band, p, look.pixels, look.palette.lit.copy(alpha = 0.55f))
            }
        },
    )
}

// endregion

// region Dot-matrix text

private typealias Glyph = List<String>

internal object DotFont {
    fun glyphs(text: String): List<Glyph> = text.uppercase().map(::glyph)

    /** Grid columns [glyphs] take at scale 1. */
    fun columns(glyphs: List<Glyph>) = glyphs.sumOf { it[0].length } + (glyphs.size - 1).coerceAtLeast(0)

    private fun glyph(ch: Char): Glyph = map[ch]
        ?: Normalizer.normalize(ch.toString(), Normalizer.Form.NFD).firstOrNull()?.let(map::get)
        ?: map.getValue(' ')

    /** Stable value in 0..<1 per dot: the order dots switch in a dither. Same as iOS. */
    fun hash(i: Int, r: Int, c: Int): Double {
        val x = sin((i * 35 + r * 5 + c) * 12.9898) * 43758.5453
        return x - floor(x)
    }

    /** 5×7, narrower where the letter allows (I, punctuation, space). Same glyphs as iOS `DotFont`. */
    private val map: Map<Char, Glyph> = mapOf(
        ' ' to listOf("00", "00", "00", "00", "00", "00", "00"),
        '0' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
        '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to listOf("11111", "00010", "00100", "00010", "00001", "10001", "01110"),
        '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
        '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
        '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
        'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
        'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
        'C' to listOf("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
        'D' to listOf("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
        'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
        'F' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
        'G' to listOf("01110", "10001", "10000", "10111", "10001", "10001", "01111"),
        'H' to listOf("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
        'I' to listOf("111", "010", "010", "010", "010", "010", "111"),
        'J' to listOf("00111", "00010", "00010", "00010", "00010", "10010", "01100"),
        'K' to listOf("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
        'L' to listOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
        'M' to listOf("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
        'N' to listOf("10001", "10001", "11001", "10101", "10011", "10001", "10001"),
        'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
        'P' to listOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
        'Q' to listOf("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
        'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
        'S' to listOf("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
        'T' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
        'U' to listOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
        'V' to listOf("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
        'W' to listOf("10001", "10001", "10001", "10101", "10101", "10101", "01010"),
        'X' to listOf("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
        'Y' to listOf("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
        'Z' to listOf("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
        '\'' to listOf("1", "1", "0", "0", "0", "0", "0"),
        '’' to listOf("1", "1", "0", "0", "0", "0", "0"),
        ':' to listOf("0", "1", "0", "0", "0", "1", "0"),
        '·' to listOf("0", "0", "0", "1", "0", "0", "0"),
        '!' to listOf("1", "1", "1", "1", "1", "0", "1"),
        '-' to listOf("000", "000", "000", "111", "000", "000", "000"),
        '−' to listOf("000", "000", "000", "111", "000", "000", "000"),
        '+' to listOf("000", "000", "010", "111", "010", "000", "000"),
        '=' to listOf("000", "000", "111", "000", "111", "000", "000"),
    )
}

/**
 * Lights [glyphs] on the grid from cell ([col], [row]), each font dot an [s]×[s] block. With [morph] < 1 a
 * share of the dots still shows [old] (the halve dither); [test] lights the bottom share of the rows in full
 * (the lamp test).
 */
private fun DrawScope.drawGlyphs(
    glyphs: List<Glyph>, old: List<Glyph>?, origin: Offset, p: Float, s: Int, col: Int, row: Int,
    look: LedLook, color: Color, glow: Boolean, morph: Float = 1f, test: Float = 0f,
) {
    val points = ArrayList<Offset>()
    val litFrom = 7 * (1 - test)
    var x = col
    glyphs.forEachIndexed { i, g ->
        val w = g[0].length
        for (r in 0 until 7) for (c in 0 until w) {
            var on = g[r][c] == '1'
            if (old != null && i < old.size && c < old[i][0].length && DotFont.hash(i, r, c) >= morph) on = old[i][r][c] == '1'
            if (r >= litFrom) on = true
            if (!on) continue
            for (dy in 0 until s) for (dx in 0 until s) points += center(origin, x + c * s + dx, row + r * s + dy, p)
        }
        x += (w + 1) * s
    }
    if (glow) drawDots(points, p, look.pixels, look.glow(color).copy(alpha = 0.35f), scale = 1.9f)
    drawDots(points, p, look.pixels, color)
}

/**
 * Text that lights the grid's own dots, on a 5×7 font: each font dot a block of grid dots, the largest up to
 * [maxScale] that fits the width. Only for short, large text (titles, totals).
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalLed.current.lit,
    glow: Boolean = true,
    maxScale: Int = 1,
    previous: String? = null,
    morph: Float = 1f,
    test: Float = 0f,
) {
    val look = LocalLed.current
    val pitch = LocalLedPitch.current
    val glyphs = remember(text) { DotFont.glyphs(text) }
    val old = remember(previous) { previous?.let(DotFont::glyphs) }
    val columns = DotFont.columns(glyphs).coerceAtLeast(1)
    BoxWithConstraints(modifier.clearAndSetSemantics { contentDescription = text.trim() }) {
        val p = with(LocalDensity.current) { pitch.toPx() }
        val fit = if (constraints.hasBoundedWidth) ((constraints.maxWidth / p - 1) / columns).toInt() else maxScale
        val s = fit.coerceIn(1, maxScale)
        // One extra pitch each way: snapping to the grid moves the text by up to one dot.
        GridCanvas(Modifier.size(pitch * (columns * s + 1), pitch * (7 * s + 1))) { o, px ->
            drawGlyphs(glyphs, old, o, px, s, 0, 0, look, color, glow, morph, test)
        }
    }
}

/** A total on the grid, padded to four cells so the width doesn't change while counting. */
@Composable
fun DotMatrixNumber(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalLed.current.lit,
    glow: Boolean = false,
    maxScale: Int = 3,
    previous: Int? = null,
    morph: Float = 1f,
    test: Float = 0f,
) {
    val width = maxOf(4, value.toString().length, previous?.toString()?.length ?: 0)
    DotMatrixText(
        value.toString().padStart(width), modifier, color, glow, maxScale,
        previous?.toString()?.padStart(width), morph, test,
    )
}

// endregion

// region Small text and readouts

/** Small panel text, too small for the grid: monospaced, uppercase, lit, with a halo. Follows font scale. */
@Composable
fun LedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalLed.current.lit,
    glow: Boolean = true,
) {
    val look = LocalLed.current
    Text(
        text.uppercase(),
        modifier,
        style = style.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = color,
            shadow = if (glow) Shadow(look.glow(color).copy(alpha = 0.8f), blurRadius = 12f) else null,
        ),
    )
}

/** A seven-segment readout (DSEG7): the unlit "8"s stay faintly visible behind the value. */
@Composable
fun SegmentReadout(
    text: String,
    digits: Int,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = LocalLed.current.lit,
) {
    val look = LocalLed.current
    val ghost = "8".repeat((digits - text.length).coerceAtLeast(0)) + text.map { if (it.isDigit() || it == '-') '8' else it }.joinToString("")
    val style = TextStyle(fontFamily = Led.segmentFont, fontSize = size)
    Box(modifier.clearAndSetSemantics { contentDescription = text }, contentAlignment = Alignment.CenterEnd) {
        Text(ghost, style = style.copy(color = look.palette.unlit), maxLines = 1)
        Text(text, style = style.copy(color = color, shadow = Shadow(look.glow(color).copy(alpha = 0.8f), blurRadius = size.value * 0.5f)), maxLines = 1)
    }
}

// endregion

// region Outside the app

/**
 * A still picture of the matrix for widgets (SPEC §6), which can't run Compose drawing: the unlit grid, then
 * [content] lights cells with [LedCanvas.text]. Same dots and font as the app.
 */
fun ledBitmap(width: Int, height: Int, density: Density, look: LedLook, pitch: Dp, content: LedCanvas.() -> Unit): Bitmap {
    val image = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
    val size = Size(image.width.toFloat(), image.height.toFloat())
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), size) {
        val p = with(density) { pitch.toPx() }
        drawRect(Led.background)
        drawDots(gridPoints(Offset.Zero, p, size), p, look.pixels, look.palette.unlit)
        LedCanvas(this, look, p).content()
    }
    return image.asAndroidBitmap()
}

class LedCanvas internal constructor(private val scope: DrawScope, val look: LedLook, private val p: Float) {
    val columns get() = (scope.size.width / p).toInt()
    val rows get() = (scope.size.height / p).toInt()

    /** Columns [text] takes at scale 1. */
    fun width(text: String) = DotFont.columns(DotFont.glyphs(text))

    /** Lights [text] with its top at [row] at the largest scale up to [maxScale] that fits; [align] 0 left, 0.5 centred, 1 right. */
    fun text(text: String, row: Int, color: Color, maxScale: Int = 1, glow: Boolean = false, align: Float = 0.5f): Int {
        val glyphs = DotFont.glyphs(text)
        val w = DotFont.columns(glyphs).coerceAtLeast(1)
        val s = (columns / w).coerceIn(1, maxScale)
        scope.drawGlyphs(glyphs, null, Offset.Zero, p, s, ((columns - w * s) * align).toInt(), row, look, color, glow)
        return 7 * s
    }
}

// endregion
