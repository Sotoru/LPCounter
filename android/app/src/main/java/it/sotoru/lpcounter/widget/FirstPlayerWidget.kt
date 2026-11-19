package it.sotoru.lpcounter.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.gameRepository
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedLook
import it.sotoru.lpcounter.ui.led.dim
import it.sotoru.lpcounter.ui.led.lit

private const val STEP = 1000
private val DeltaKey = ActionParameters.Key<Int>("delta")
/** Room for the − / + row under the total. */
private val ButtonsHeight = 52.dp

/** Player 1's total on the matrix with − / + 1000 (SPEC §6), through the same rules as the app. */
class FirstPlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lp = context.lifePoints().one
        val look = context.ledLook()
        provideContent { Content(context, lp, look) }
    }

    @Composable
    private fun Content(context: Context, lp: Int, look: LedLook) {
        val size = LocalSize.current
        val bitmap = context.widgetBitmap(size, look) {
            val free = rows - (ButtonsHeight.value / 3).toInt()
            val top = ((free - 9 - 21) / 2).coerceAtLeast(1)
            text("LP1", top, look.dim)
            text(lp.toString().padStart(4), top + 9, if (lp == 0) Led.red else look.lit, maxScale = 3)
        }
        Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Image(
                ImageProvider(bitmap),
                context.getString(R.string.widget_first_player_content, lp),
                GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Row(GlanceModifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Step("−", -STEP, Led.red, GlanceModifier.defaultWeight().description(context.getString(R.string.action_subtract_1000)))
                Spacer(GlanceModifier.width(8.dp))
                Step("+", STEP, Led.green, GlanceModifier.defaultWeight().description(context.getString(R.string.action_add_1000)))
            }
        }
    }

    @Composable
    private fun Step(sign: String, delta: Int, color: androidx.compose.ui.graphics.Color, modifier: GlanceModifier) {
        FilledButton(
            text = sign,
            onClick = actionRunCallback<ChangeLifePointsAction>(actionParametersOf(DeltaKey to delta)),
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(color, color),
                contentColor = ColorProvider(Led.background, Led.background),
            ),
        )
    }
}

private fun GlanceModifier.description(text: String) = semantics { contentDescription = text }

class ChangeLifePointsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[DeltaKey] ?: return
        context.gameRepository.update(Player.One) { it + delta }
    }
}

class FirstPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FirstPlayerWidget()
}
