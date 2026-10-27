package it.sotoru.lpcounter.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import it.sotoru.lpcounter.MainActivity
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.LifePoints
import it.sotoru.lpcounter.gameRepository
import kotlinx.coroutines.flow.first

/** Read-only preview of both scores. */
class GameWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lp = context.gameRepository.lifePoints.first()
        provideContent { GlanceTheme { Content(context, lp) } }
    }

    @Composable
    private fun Content(context: Context, lp: LifePoints) {
        val text = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 20.sp)
        val value = text.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(8.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(context.getString(R.string.lp_label, 2), style = text)
            Text(lp.two.toString(), style = value)
            Spacer(GlanceModifier.height(12.dp))
            Text(context.getString(R.string.lp_label, 1), style = text)
            Text(lp.one.toString(), style = value)
        }
    }
}

class GameWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = GameWidget()
}
