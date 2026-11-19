package it.sotoru.lpcounter.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import it.sotoru.lpcounter.MainActivity
import it.sotoru.lpcounter.R
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import it.sotoru.lpcounter.data.LifePoints
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedLook
import it.sotoru.lpcounter.ui.led.lit
import it.sotoru.lpcounter.widget.ledLook
import it.sotoru.lpcounter.widget.lifePoints
import it.sotoru.lpcounter.widget.widgetBitmap
import kotlinx.coroutines.launch

/** Android counterpart of the iOS Live Activity: an ongoing notification with both scores. */
object LiveScoreNotifier {
    private const val CHANNEL_ID = "live_score"
    private const val NOTIFICATION_ID = 1

    suspend fun show(context: Context) = post(context, context.lifePoints(), context.ledLook())

    /** Updates the notification only if it is currently showing. */
    suspend fun refresh(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.activeNotifications.any { it.id == NOTIFICATION_ID }) show(context)
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    private fun post(context: Context, lp: LifePoints, look: LedLook) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_live_score),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lp)
            .setContentTitle(context.getString(R.string.live_score_title))
            .setContentText(context.getString(R.string.live_score_text, lp.one, lp.two))
            .setContentIntent(open)
            .setColor(look.lit.toArgb())
            // Expanded, the totals on the LED matrix (SPEC §7); collapsed, the plain text above.
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(matrix(context, lp, look)))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/** `LP·COUNTER` and both totals, Player 1 on the left like the landscape board. */
private fun matrix(context: Context, lp: LifePoints, look: LedLook) =
    context.widgetBitmap(DpSize(360.dp, 120.dp), look) {
        text("LP·COUNTER", 3, look.lit, glow = true, align = 0.04f)
        text(lp.one.toString(), 16, if (lp.one == 0) Led.red else look.lit, maxScale = 3, align = 0.04f)
        text(lp.two.toString(), 16, if (lp.two == 0) Led.red else look.lit, maxScale = 3, align = 0.96f)
    }

/** Asks once for the notification permission (API 33+) and shows the ongoing score. */
@Composable
fun ShowLiveScore() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun show() {
        scope.launch { LiveScoreNotifier.show(context) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) show() }

    LaunchedEffect(Unit) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (!needsPermission) return@LaunchedEffect show()
        // Ask only once: a denial is respected instead of re-prompting on every launch.
        val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("asked", false)) {
            prefs.edit().putBoolean("asked", true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
