@file:OptIn(ExperimentalMaterial3Api::class)

package it.sotoru.lpcounter.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.DIM_AFTER_OPTIONS
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.STARTING_LIFE_POINTS_OPTIONS
import it.sotoru.lpcounter.data.Settings
import it.sotoru.lpcounter.data.TIMER_PRESETS
import it.sotoru.lpcounter.feature.game.GameViewModel
import it.sotoru.lpcounter.feature.timer.timerLabel
import it.sotoru.lpcounter.ui.led.DotMatrixNumber
import it.sotoru.lpcounter.ui.led.Led
import it.sotoru.lpcounter.ui.led.LedMatrix
import it.sotoru.lpcounter.ui.led.LedPalette
import it.sotoru.lpcounter.ui.led.LedText
import it.sotoru.lpcounter.ui.led.LocalLedPitch
import it.sotoru.lpcounter.ui.led.PixelStyle

/** Opened from the Home's gear (SPEC §5), grouped by what the settings touch. */
@Composable
fun SettingsScreen(state: GameState, settings: Settings, viewModel: GameViewModel, onDone: () -> Unit) {
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Box(Modifier.fillMaxSize()) {
        LedMatrix()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    actions = {
                        IconButton(onClick = onDone) { Icon(Icons.Rounded.Check, stringResource(R.string.action_done)) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Led.background),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                SectionHeader(R.string.settings_section_duel)
                Choice(
                    Icons.Rounded.Favorite, R.string.settings_starting_lp, null,
                    STARTING_LIFE_POINTS_OPTIONS, state.startingLifePoints,
                    { if (it == 4000) stringResource(R.string.speed_duel_option) else it.toString() },
                    viewModel::setStartingLifePoints,
                )
                val timers = listOf(0) + TIMER_PRESETS + listOfNotNull(state.timerMinutes.takeIf { it != 0 && it !in TIMER_PRESETS })
                Choice(Icons.Rounded.Timer, R.string.settings_timer, null, timers, state.timerMinutes, { timerLabel(it) }, viewModel::setTimerMinutes)
                SwitchRow(Icons.Rounded.Flag, R.string.settings_auto_end, R.string.settings_auto_end_hint, settings.autoEnd, viewModel::setAutoEnd)
                Footer(R.string.settings_duel_footer)

                SectionHeader(R.string.settings_section_duel_screen)
                SwitchRow(Icons.Rounded.RestartAlt, R.string.settings_confirm_reset, R.string.settings_confirm_reset_hint, settings.confirmReset, viewModel::setConfirmReset)
                SwitchRow(Icons.Rounded.Vibration, R.string.settings_haptics, R.string.settings_haptics_hint, settings.haptics, viewModel::setHaptics)
                Choice(
                    Icons.Rounded.DarkMode, R.string.settings_dim, R.string.settings_dim_hint,
                    DIM_AFTER_OPTIONS, settings.dimAfter, { dimLabel(it) }, viewModel::setDimAfter,
                )
                Footer(R.string.settings_duel_screen_footer)

                SectionHeader(R.string.settings_section_appearance)
                // The duel's own dots, larger, so the preview shows them as they will look at the table.
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalLedPitch provides 12.dp) {
                        LedMatrix()
                        DotMatrixNumber(state.startingLifePoints, Modifier.padding(vertical = 12.dp), maxScale = 1)
                    }
                }
                Choice(
                    null, R.string.settings_palette, null, LedPalette.entries, settings.palette,
                    { stringResource(it.title) }, viewModel::setPalette,
                    swatch = { Box(Modifier.size(20.dp).background(it.lit, CircleShape)) },
                )
                ListItem(headlineContent = { Text(stringResource(R.string.settings_pixels)) }, colors = rowColors())
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().background(Led.panel).padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    PixelStyle.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == settings.pixels,
                            onClick = { viewModel.setPixels(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, PixelStyle.entries.size),
                        ) { Text(stringResource(option.title)) }
                    }
                }
                Footer(R.string.settings_appearance_footer)

                SectionHeader(R.string.settings_section_data)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_clear_data), color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { confirmingClear = true },
                    colors = rowColors(),
                )
                Footer(R.string.settings_clear_data_hint)

                SectionHeader(R.string.settings_section_about)
                val context = LocalContext.current
                val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "–"
                ListItem(headlineContent = { Text(stringResource(R.string.settings_version)) }, trailingContent = { Text(version) }, colors = rowColors())
                ListItem(headlineContent = { Text(stringResource(R.string.settings_fonts)) }, trailingContent = { Text("DSEG7 · OFL") }, colors = rowColors())
            }
        }
    }

    if (confirmingClear) {
        AlertDialog(
            onDismissRequest = { confirmingClear = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null) },
            title = { Text(stringResource(R.string.clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_data_hint)) },
            confirmButton = {
                TextButton(onClick = { confirmingClear = false; viewModel.clearAll() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun dimLabel(seconds: Int) = when {
    seconds == 0 -> stringResource(R.string.dim_never)
    seconds < 60 -> stringResource(R.string.dim_seconds, seconds)
    else -> stringResource(R.string.timer_minutes, seconds / 60)
}

@Composable
private fun rowColors() = ListItemDefaults.colors(containerColor = Led.panel)

@Composable
private fun SectionHeader(title: Int) {
    LedText(
        stringResource(title),
        MaterialTheme.typography.labelLarge,
        Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun Footer(text: Int) {
    Text(
        stringResource(text),
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SwitchRow(icon: ImageVector, title: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(hint)) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
        colors = rowColors(),
    )
}

/** A row that opens a menu of [options]; the current one is shown on the right. */
@Composable
private fun <T> Choice(
    icon: ImageVector?,
    title: Int,
    hint: Int?,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    swatch: (@Composable (T) -> Unit)? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = hint?.let { { Text(stringResource(it)) } },
        leadingContent = icon?.let { { Icon(it, null) } } ?: swatch?.let { { it(selected) } },
        trailingContent = {
            Box {
                Text(label(selected), color = MaterialTheme.colorScheme.primary)
                DropdownMenu(open, { open = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(label(option)) },
                            onClick = { onSelect(option); open = false },
                            leadingIcon = swatch?.let { { it(option) } },
                            trailingIcon = { if (option == selected) Icon(Icons.Rounded.Check, null) },
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { open = true },
        colors = rowColors(),
    )
}
