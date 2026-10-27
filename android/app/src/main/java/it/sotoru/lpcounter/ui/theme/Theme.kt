package it.sotoru.lpcounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Dynamic color needs Android 12 (S); minSdk is 30, so Android 11 falls back to the baseline scheme.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LPCounterTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colorScheme = when {
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ->
            if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
        dark -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
