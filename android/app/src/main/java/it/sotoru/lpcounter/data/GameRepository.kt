package it.sotoru.lpcounter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import it.sotoru.lpcounter.notification.LiveScoreNotifier
import it.sotoru.lpcounter.widget.FirstPlayerWidget
import it.sotoru.lpcounter.widget.GameWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "game")

private val KeyP1 = intPreferencesKey("lifePointsP1")
private val KeyP2 = intPreferencesKey("lifePointsP2")
private val HistoryP1 = stringPreferencesKey("historyP1")
private val HistoryP2 = stringPreferencesKey("historyP2")

// ponytail: bounded so the persisted string stays small; reset-game keeps history across games.
private const val MAX_HISTORY = 200

private val Player.lifeKey get() = if (this == Player.One) KeyP1 else KeyP2
private val Player.historyKey get() = if (this == Player.One) HistoryP1 else HistoryP2

/**
 * Single source of truth for life points and their history, shared by the app, widgets and
 * notification. History lives here (not in the UI) so changes made from the widget can be undone.
 */
class GameRepository(
    private val store: DataStore<Preferences>,
    // Widgets and the notification live outside the app UI, so they are pushed on every write.
    private val onChange: suspend (LifePoints) -> Unit = {},
) {
    val lifePoints: Flow<LifePoints> = store.data.map { it.toLifePoints() }

    val game: Flow<GameState> = store.data.map { prefs ->
        GameState(prefs.toLifePoints(), Player.entries.associateWith { prefs.history(it) })
    }

    /** Atomically applies [transform] (result clamped at 0) and records the change actually applied. */
    suspend fun update(player: Player, transform: (Int) -> Int) {
        val prefs = store.edit {
            val old = it[player.lifeKey] ?: INITIAL_LIFE_POINTS
            val new = transform(old).coerceAtLeast(0)
            it[player.lifeKey] = new
            if (new != old) it.setHistory(player, (it.history(player) + (new - old)).takeLast(MAX_HISTORY))
        }
        onChange(prefs.toLifePoints())
    }

    /** Reverts the player's most recent change, relative to the current total. */
    suspend fun undo(player: Player) {
        val prefs = store.edit {
            val history = it.history(player)
            val last = history.lastOrNull() ?: return@edit
            it[player.lifeKey] = ((it[player.lifeKey] ?: INITIAL_LIFE_POINTS) - last).coerceAtLeast(0)
            it.setHistory(player, history.dropLast(1))
        }
        onChange(prefs.toLifePoints())
    }

    /** Resets one player's total and clears their history. */
    suspend fun reset(player: Player) {
        val prefs = store.edit {
            it[player.lifeKey] = INITIAL_LIFE_POINTS
            it.setHistory(player, emptyList())
        }
        onChange(prefs.toLifePoints())
    }

    /** Resets both totals; history is left untouched. */
    suspend fun reset() {
        val prefs = store.edit {
            it[KeyP1] = INITIAL_LIFE_POINTS
            it[KeyP2] = INITIAL_LIFE_POINTS
        }
        onChange(prefs.toLifePoints())
    }

    private fun Preferences.toLifePoints() =
        LifePoints(this[KeyP1] ?: INITIAL_LIFE_POINTS, this[KeyP2] ?: INITIAL_LIFE_POINTS)

    private fun Preferences.history(player: Player): List<Int> =
        this[player.historyKey]?.split(',')?.filter(String::isNotEmpty)?.map(String::toInt).orEmpty()

    private fun MutablePreferences.setHistory(player: Player, list: List<Int>) {
        if (list.isEmpty()) remove(player.historyKey) else this[player.historyKey] = list.joinToString(",")
    }
}

fun GameRepository(context: Context): GameRepository {
    val appContext = context.applicationContext
    return GameRepository(appContext.dataStore) { lp ->
        GameWidget().updateAll(appContext)
        FirstPlayerWidget().updateAll(appContext)
        LiveScoreNotifier.refresh(appContext, lp)
    }
}
