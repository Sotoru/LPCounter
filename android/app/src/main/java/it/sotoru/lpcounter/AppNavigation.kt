package it.sotoru.lpcounter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.sotoru.lpcounter.feature.game.GameScreen
import it.sotoru.lpcounter.feature.game.GameViewModel
import it.sotoru.lpcounter.feature.home.HomeScreen
import it.sotoru.lpcounter.feature.settings.SettingsScreen

private enum class Screen { Home, Settings, Game }

/** Root is the Home, no tabs (SPEC §9): Play pushes the full-screen game, the gear opens Settings over it. */
// shortcut: three fixed destinations, so an enum is the back stack; Navigation 3 when there are more.
@Composable
fun AppNavigation(viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val resets by viewModel.resets.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    val game = state ?: return
    val prefs = settings ?: return
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            when {
                targetState == Screen.Settings -> slideInVertically { it } + fadeIn() togetherWith fadeOut()
                initialState == Screen.Settings -> fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                targetState == Screen.Game -> slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                else -> slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
            }
        },
        label = "navigation",
    ) { current ->
        when (current) {
            Screen.Home -> HomeScreen(game, viewModel, onPlay = { screen = Screen.Game }, onSettings = { screen = Screen.Settings })
            Screen.Settings -> {
                BackHandler { screen = Screen.Home }
                SettingsScreen(game, prefs, viewModel, onDone = { screen = Screen.Home })
            }
            Screen.Game -> GameScreen(game, prefs, resets, viewModel, onHome = { screen = Screen.Home })
        }
    }
}
