package it.sotoru.lpcounter.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import it.sotoru.lpcounter.gameRepository
import it.sotoru.lpcounter.notification.LiveScoreNotifier
import it.sotoru.lpcounter.settingsRepository
import it.sotoru.lpcounter.ui.led.LedCanvas
import it.sotoru.lpcounter.ui.led.LedLook
import it.sotoru.lpcounter.ui.led.ledBitmap
import kotlinx.coroutines.flow.first
import androidx.compose.ui.unit.Density

/** Widgets and the notification live outside the app UI, so they are redrawn on every write. */
suspend fun refreshSurfaces(context: Context) {
    GameWidget().updateAll(context)
    FirstPlayerWidget().updateAll(context)
    LiveScoreNotifier.refresh(context)
}

/** Colour and dot shape chosen in the app (SPEC §5), for drawing outside it. */
suspend fun Context.ledLook(): LedLook = settingsRepository.settings.first().let { LedLook(it.palette, it.pixels) }

suspend fun Context.lifePoints() = gameRepository.lifePoints.first()

/** The matrix, a widget's size, with [content] lit on it (SPEC §10: 3 dp pitch outside the app). */
fun Context.widgetBitmap(size: DpSize, look: LedLook, content: LedCanvas.() -> Unit) =
    resources.displayMetrics.density.let { d ->
        ledBitmap((size.width.value * d).toInt(), (size.height.value * d).toInt(), Density(d), look, 3.dp, content)
    }
