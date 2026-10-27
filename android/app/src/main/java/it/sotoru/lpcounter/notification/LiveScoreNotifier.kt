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
import it.sotoru.lpcounter.data.LifePoints
import it.sotoru.lpcounter.gameRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Android counterpart of the iOS Live Activity: an ongoing notification with both scores. */
object LiveScoreNotifier {
    private const val CHANNEL_ID = "live_score"
    private const val NOTIFICATION_ID = 1

    fun show(context: Context, lp: LifePoints) = post(context, lp)

    /** Updates the notification only if it is currently showing. */
    fun refresh(context: Context, lp: LifePoints) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.activeNotifications.any { it.id == NOTIFICATION_ID }) post(context, lp)
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    private fun post(context: Context, lp: LifePoints) {
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
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/** Asks once for the notification permission (API 33+) and shows the ongoing score. */
@Composable
fun ShowLiveScore() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun show() {
        scope.launch { LiveScoreNotifier.show(context, context.gameRepository.lifePoints.first()) }
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
