@file:OptIn(ExperimentalMaterial3Api::class)

package it.sotoru.lpcounter.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.AppTheme
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.STARTING_LIFE_POINTS_OPTIONS
import it.sotoru.lpcounter.data.TIMER_OPTIONS
import it.sotoru.lpcounter.data.Settings
import it.sotoru.lpcounter.feature.game.GameViewModel

/** Second root tab (SPEC §5). */
@Composable
fun SettingsScreen(state: GameState, settings: Settings, viewModel: GameViewModel) {
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(R.string.settings_section_duel_in_progress)
            SwitchRow(R.string.settings_confirm_reset, R.string.settings_confirm_reset_hint, settings.confirmReset, viewModel::setConfirmReset)
            SwitchRow(R.string.settings_auto_end, R.string.settings_auto_end_hint, settings.autoEnd, viewModel::setAutoEnd)
            SwitchRow(R.string.settings_haptics, R.string.settings_haptics_hint, settings.haptics, viewModel::setHaptics)

            SectionHeader(R.string.settings_section_appearance)
            ListItem(headlineContent = { Text(stringResource(R.string.settings_theme)) })
            Segmented(
                options = AppTheme.entries,
                selected = settings.theme,
                label = { stringResource(it.label) },
                onSelect = viewModel::setTheme,
            )

            SectionHeader(R.string.settings_section_duel)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_starting_lp)) },
                supportingContent = { Text(stringResource(R.string.settings_starting_lp_hint)) },
            )
            Segmented(STARTING_LIFE_POINTS_OPTIONS, state.startingLifePoints, { it.toString() }, viewModel::setStartingLifePoints)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_timer)) },
                supportingContent = { Text(stringResource(R.string.settings_timer_hint)) },
            )
            Segmented(
                TIMER_OPTIONS,
                state.timerMinutes,
                { if (it == 0) stringResource(R.string.timer_off) else stringResource(R.string.timer_minutes, it) },
                viewModel::setTimerMinutes,
            )

            SectionHeader(R.string.settings_section_data)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_data), color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text(stringResource(R.string.settings_clear_data_hint)) },
                leadingContent = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { confirmingClear = true },
            )
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

private val AppTheme.label
    get() = when (this) {
        AppTheme.System -> R.string.theme_system
        AppTheme.Light -> R.string.theme_light
        AppTheme.Dark -> R.string.theme_dark
    }

@Composable
internal fun SectionHeader(title: Int) {
    Text(
        stringResource(title),
        Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SwitchRow(title: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(hint)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.padding(0.dp),
    )
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label(option)) }
        }
    }
}
