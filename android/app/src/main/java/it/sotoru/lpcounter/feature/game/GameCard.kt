package it.sotoru.lpcounter.feature.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.sotoru.lpcounter.R
import it.sotoru.lpcounter.data.INITIAL_LIFE_POINTS
import kotlin.math.abs

/** One player's card: a pager with the counter and the history. */
@Composable
fun GameCard(
    lifePoints: Int,
    history: List<Int>,
    inverted: Boolean,
    isLandscape: Boolean,
    onSetLifePoints: (Int) -> Unit,
    onHalve: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Card(
        modifier = modifier
            .padding(16.dp)
            // Players face each other across the device in portrait; in landscape both read upright.
            .rotate(if (inverted && !isLandscape) 180f else 0f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(pagerState, Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> CounterPage(lifePoints, inverted, isLandscape, onSetLifePoints, onHalve, onUndo, onReset, history.isEmpty())
                    else -> HistoryPage(history)
                }
            }
            // Material has no pager indicator, so this is the minimal dot row in theme colors.
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(2) { i ->
                    val selected = pagerState.currentPage == i
                    Surface(
                        Modifier.size(8.dp),
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ) {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CounterPage(
    lifePoints: Int,
    inverted: Boolean,
    isLandscape: Boolean,
    onSetLifePoints: (Int) -> Unit,
    onHalve: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    historyEmpty: Boolean,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            lifePoints.toString(),
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.action_open_calculator)) { showSheet = true }
                .padding(bottom = 24.dp),
            style = MaterialTheme.typography.displayLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 40.sp, maxFontSize = 100.sp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupSpacing)) {
            val colors = MaterialTheme.colorScheme
            CounterButton(Icons.Rounded.Clear, R.string.action_reset_player,
                IconButtonDefaults.filledIconButtonColors(colors.errorContainer, colors.onErrorContainer),
                enabled = lifePoints != INITIAL_LIFE_POINTS, onClick = onReset)
            CounterButton(Icons.AutoMirrored.Rounded.Undo, R.string.action_undo,
                IconButtonDefaults.filledIconButtonColors(colors.secondaryContainer, colors.onSecondaryContainer),
                enabled = !historyEmpty, onClick = onUndo)
            CounterButton(Icons.Rounded.Percent, R.string.action_halve,
                IconButtonDefaults.filledIconButtonColors(colors.tertiaryContainer, colors.onTertiaryContainer),
                enabled = true, onClick = onHalve)
        }
    }

    if (showSheet) {
        CalculatorSheet(
            lifePoints = lifePoints,
            isLandscape = isLandscape,
            inverted = inverted,
            onSubmit = onSetLifePoints,
            onDismiss = { showSheet = false },
        )
    }
}

private val ButtonGroupSpacing = 8.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CounterButton(
    icon: ImageVector,
    description: Int,
    colors: IconButtonColors,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
        shapes = IconButtonDefaults.shapes(),
        colors = colors,
    ) { Icon(icon, stringResource(description)) }
}

@Composable
private fun HistoryPage(history: List<Int>) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_history), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        // Newest first; entries are signed changes, so duplicates are possible and index is the key.
        itemsIndexed(history.asReversed(), key = { index, _ -> index }) { _, number ->
            val isPositive = number > 0
            ListItem(
                headlineContent = {
                    Text(
                        "${if (isPositive) "+" else "-"} ${abs(number)}",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                ),
            )
            HorizontalDivider()
        }
    }
}
