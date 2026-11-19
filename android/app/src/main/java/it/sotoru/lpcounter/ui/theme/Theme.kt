package it.sotoru.lpcounter.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedLook
import it.sotoru.lpcounter.ui.led.LocalLed

/** Always dark (SPEC §10: no theme): black with the chosen LED colour as accent; controls stay Material. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LPCounterTheme(look: LedLook = LedLook(), content: @Composable () -> Unit) {
    val p = look.palette
    val colorScheme = darkColorScheme(
        primary = p.lit,
        onPrimary = Color.Black,
        primaryContainer = p.halo.copy(alpha = 0.35f).compositeOverBlack(),
        onPrimaryContainer = p.lit,
        secondaryContainer = p.unlit,
        onSecondaryContainer = p.lit,
        tertiary = Led.green,
        error = Led.red,
        onError = Color.Black,
        errorContainer = Led.red.copy(alpha = 0.25f).compositeOverBlack(),
        onErrorContainer = Led.red,
        background = Led.background,
        surface = Led.background,
        surfaceContainerLowest = Led.background,
        surfaceContainerLow = p.unlit,
        surfaceContainer = p.unlit,
        surfaceContainerHigh = p.unlit,
        surfaceContainerHighest = p.unlit,
        onSurface = p.lit,
        onSurfaceVariant = p.lit.copy(alpha = 0.6f).compositeOverBlack(),
        outline = p.lit.copy(alpha = 0.4f).compositeOverBlack(),
        outlineVariant = p.lit.copy(alpha = 0.2f).compositeOverBlack(),
    )
    CompositionLocalProvider(LocalLed provides look) {
        MaterialExpressiveTheme(colorScheme = colorScheme, motionScheme = MotionScheme.expressive(), content = content)
    }
}

private fun Color.compositeOverBlack() = Color(red * alpha, green * alpha, blue * alpha)
