@file:OptIn(ExperimentalMaterial3Api::class)

package it.sotoru.lpcounter.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.feature.game.GameViewModel
import it.sotoru.lpcounter.feature.settings.SectionHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** First tab (SPEC §9): Play, and recent duels grouped by day below. */
@Composable
fun HomeScreen(state: GameState, viewModel: GameViewModel, onPlay: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val zone = remember { ZoneId.systemDefault() }
    val dayFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val days = remember(state.duels) {
        state.duels.groupBy { Instant.ofEpochMilli(it.endedAt).atZone(zone).toLocalDate() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text(stringResource(R.string.app_name)) }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                Button(
                    onClick = { viewModel.startDuel(); onPlay() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Text(stringResource(R.string.action_play), style = MaterialTheme.typography.titleMedium)
                }
                SectionHeader(R.string.home_section_recent)
                if (days.isEmpty()) ListItem(headlineContent = { Text(stringResource(R.string.no_recent_duels)) })
            }
            days.forEach { (day, duels) ->
                item(key = day.toEpochDay()) {
                    Text(
                        day.format(dayFormat),
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                items(duels, key = { it.endedAt }) { duel ->
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.duel_row, duel.one, duel.two)) },
                        trailingContent = { Text(Instant.ofEpochMilli(duel.endedAt).atZone(zone).format(timeFormat)) },
                    )
                }
            }
        }
    }
}
