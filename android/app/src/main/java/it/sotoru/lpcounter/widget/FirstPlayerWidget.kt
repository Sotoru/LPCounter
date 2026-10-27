package it.sotoru.lpcounter.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.gameRepository
import kotlinx.coroutines.flow.first

private const val STEP = 1000
private val DeltaKey = ActionParameters.Key<Int>("delta")
private val Small = DpSize(110.dp, 110.dp)
private val Large = DpSize(250.dp, 250.dp)

/** Player 1 score with interactive -/+ buttons. */
class FirstPlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Large))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lp = context.gameRepository.lifePoints.first().one
        provideContent { GlanceTheme { Content(context, lp) } }
    }

    @Composable
    private fun Content(context: Context, lp: Int) {
        val large = LocalSize.current.height >= Large.height
        val color = GlanceTheme.colors.onSurface
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                context.getString(R.string.lp_label, 1),
                style = TextStyle(color = color, fontSize = if (large) 28.sp else 20.sp, fontWeight = FontWeight.Bold),
            )
            Text(lp.toString(), style = TextStyle(color = color, fontSize = if (large) 48.sp else 28.sp))
            Spacer(GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    text = "−",
                    onClick = actionRunCallback<ChangeLifePointsAction>(actionParametersOf(DeltaKey to -STEP)),
                )
                Spacer(GlanceModifier.width(8.dp))
                Button(
                    text = "+",
                    onClick = actionRunCallback<ChangeLifePointsAction>(actionParametersOf(DeltaKey to STEP)),
                )
            }
        }
    }
}

class ChangeLifePointsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[DeltaKey] ?: return
        context.gameRepository.update(Player.One) { it + delta }
    }
}

class FirstPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FirstPlayerWidget()
}
