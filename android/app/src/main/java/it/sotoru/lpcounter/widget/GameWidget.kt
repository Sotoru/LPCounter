package it.sotoru.lpcounter.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import it.sotoru.lpcounter.MainActivity
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.LifePoints
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedLook
import it.sotoru.lpcounter.ui.led.dim
import it.sotoru.lpcounter.ui.led.lit

/** Both totals on the LED matrix, read only; a tap opens the app (SPEC §6). LP2 above, LP1 below, like the board. */
class GameWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lp = context.lifePoints()
        val look = context.ledLook()
        provideContent { Content(context, lp, look) }
    }

    @Composable
    private fun Content(context: Context, lp: LifePoints, look: LedLook) {
        val bitmap = context.widgetBitmap(LocalSize.current, look) {
            // Two halves, each a label and its total, centred in its half.
            val half = rows / 2
            listOf(2 to lp.two, 1 to lp.one).forEachIndexed { i, (player, value) ->
                val top = i * half + (half - 7 - 2 - 21) / 2
                text("LP$player", top.coerceAtLeast(1), look.dim)
                text(value.toString().padStart(4), top + 9, if (value == 0) Led.red else look.lit, maxScale = 3)
            }
        }
        Image(
            ImageProvider(bitmap),
            context.getString(R.string.widget_game_content, lp.one, lp.two),
            GlanceModifier.fillMaxSize().clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            contentScale = ContentScale.FillBounds,
        )
    }
}

class GameWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = GameWidget()
}
