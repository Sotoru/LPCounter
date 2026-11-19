package it.sotoru.lpcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.sotoru.lpcounter.notification.ShowLiveScore
import it.sotoru.lpcounter.ui.led.LedLook
import it.sotoru.lpcounter.ui.theme.LPCounterTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always dark (SPEC §10): light system bar icons on the black display.
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val looks = settingsRepository.settings.map { LedLook(it.palette, it.pixels) }
        setContent {
            val look by looks.collectAsStateWithLifecycle(null)
            LPCounterTheme(look ?: return@setContent) {
                AppNavigation()
                ShowLiveScore()
            }
        }
    }
}
