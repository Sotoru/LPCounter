package it.sotoru.lpcounter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.sotoru.lpcounter.feature.game.GameScreen
import it.sotoru.lpcounter.feature.game.GameViewModel
import it.sotoru.lpcounter.feature.home.HomeScreen
import it.sotoru.lpcounter.feature.settings.SettingsScreen

/** Two tabs, Home and Settings; Play pushes the full-screen game over them (SPEC §9). */
// ponytail: one pushed screen, so a Boolean is the back stack; Navigation 3 when there are more destinations.
@Composable
fun AppNavigation(viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var playing by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val game = state ?: return
    val prefs = settings ?: return
    AnimatedContent(
        targetState = playing,
        transitionSpec = {
            if (targetState) slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
            else slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
        },
        label = "navigation",
    ) { inGame ->
        if (inGame) {
            GameScreen(game, prefs, viewModel, onHome = { playing = false })
        } else {
            // Material: back from a secondary tab goes to the start tab first.
            BackHandler(enabled = tab != 0) { tab = 0 }
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Rounded.Home, null) },
                            label = { Text(stringResource(R.string.tab_home)) },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Rounded.Settings, null) },
                            label = { Text(stringResource(R.string.settings_title)) },
                        )
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                    if (tab == 0) HomeScreen(game, viewModel, onPlay = { playing = true })
                    else SettingsScreen(game, prefs, viewModel)
                }
            }
        }
    }
}
