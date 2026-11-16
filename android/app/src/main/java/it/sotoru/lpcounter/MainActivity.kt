package it.sotoru.lpcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.sotoru.lpcounter.data.AppTheme
import kotlinx.coroutines.flow.map
import it.sotoru.lpcounter.notification.ShowLiveScore
import it.sotoru.lpcounter.ui.theme.LPCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themes = (application as LPCounterApp).settings.settings.map { it.theme }
        setContent {
            val theme by themes.collectAsStateWithLifecycle(AppTheme.System)
            LPCounterTheme(theme) {
                AppNavigation()
                ShowLiveScore()
            }
        }
    }
}
