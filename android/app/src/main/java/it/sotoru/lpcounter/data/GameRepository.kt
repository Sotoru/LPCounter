package it.sotoru.lpcounter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.sotoru.lpcounter.widget.refreshSurfaces
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "game")

private val KeyP1 = intPreferencesKey("lifePointsP1")
private val KeyP2 = intPreferencesKey("lifePointsP2")
private val HistoryP1 = stringPreferencesKey("historyP1")
private val HistoryP2 = stringPreferencesKey("historyP2")
private val StartingLifePoints = intPreferencesKey("startingLifePoints")
// Recent duels were dropped (SPEC §1): what an older version stored is removed on the next reset.
private val Duels = stringPreferencesKey("recentDuels")
private val TimerMinutes = intPreferencesKey("timerMinutes")
private val TimerEndsAt = longPreferencesKey("timerEndsAt")
private val TimerPaused = longPreferencesKey("timerPausedRemaining")

private const val MAX_HISTORY = 200

private val Player.lifeKey get() = if (this == Player.One) KeyP1 else KeyP2
private val Player.historyKey get() = if (this == Player.One) HistoryP1 else HistoryP2

/**
 * Single source of truth for life points and their history, shared by the app, widgets and
 * notification. History lives here (not in the UI) so changes made from the widget can be undone.
 */
class GameRepository(
    private val store: DataStore<Preferences>,
    private val now: () -> Long = System::currentTimeMillis,
    // Widgets and the notification live outside the app UI, so they are pushed on every write.
    private val onChange: suspend (LifePoints) -> Unit = {},
) {
    val lifePoints: Flow<LifePoints> = store.data.map { it.toLifePoints() }

    val game: Flow<GameState> = store.data.map { prefs ->
        GameState(
            prefs.toLifePoints(),
            Player.entries.associateWith { prefs.history(it) },
            prefs.starting,
            prefs[TimerMinutes] ?: 0,
            prefs[TimerEndsAt],
            prefs[TimerPaused],
        )
    }

    /** Atomically applies [transform] (result clamped at 0) and records the change actually applied. */
    suspend fun update(player: Player, transform: (Int) -> Int) {
        val prefs = store.edit {
            val old = it[player.lifeKey] ?: it.starting
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
            it[player.lifeKey] = ((it[player.lifeKey] ?: it.starting) - last).coerceAtLeast(0)
            it.setHistory(player, history.dropLast(1))
        }
        onChange(prefs.toLifePoints())
    }

    /** Resets one player's total and clears their history. */
    suspend fun reset(player: Player) {
        val prefs = store.edit {
            it[player.lifeKey] = it.starting
            it.setHistory(player, emptyList())
        }
        onChange(prefs.toLifePoints())
    }

    /** A new duel: both totals back to the start and both histories cleared (SPEC §2). */
    suspend fun reset() {
        val prefs = store.edit {
            it[KeyP1] = it.starting
            it[KeyP2] = it.starting
            it.remove(HistoryP1); it.remove(HistoryP2); it.remove(Duels)
        }
        onChange(prefs.toLifePoints())
    }

    /** Stores the starting LP; applies at once if the duel hasn't started, otherwise from the next reset (SPEC §5). */
    suspend fun setStartingLifePoints(value: Int) {
        val prefs = store.edit {
            val untouched = it.toLifePoints() == LifePoints(it.starting, it.starting)
            it[StartingLifePoints] = value
            if (untouched) {
                it[KeyP1] = value
                it[KeyP2] = value
            }
        }
        onChange(prefs.toLifePoints())
    }

    /** Clamped to [TIMER_RANGE]; "off" (0) removes the timer at once, a new length waits for Play. */
    suspend fun setTimerMinutes(value: Int) {
        store.edit {
            val minutes = if (value == 0) 0 else value.coerceIn(TIMER_RANGE)
            it[TimerMinutes] = minutes
            if (minutes == 0) it.removeTimer()
        }
    }

    /** On Play (SPEC §9): a paused timer resumes, a running one continues, otherwise a new one starts; "off" removes it. */
    suspend fun startDuel() {
        store.edit {
            val minutes = it[TimerMinutes] ?: 0
            val endsAt = it[TimerEndsAt]
            val paused = it[TimerPaused]
            when {
                minutes == 0 -> it.removeTimer()
                paused != null -> { it[TimerEndsAt] = now() + paused; it.remove(TimerPaused) }
                endsAt == null || endsAt <= now() -> it[TimerEndsAt] = now() + minutes * 60_000L
            }
        }
    }

    /** Freezes the countdown where it is; [resumeTimer] picks it up again. */
    suspend fun pauseTimer() {
        store.edit {
            val endsAt = it[TimerEndsAt] ?: return@edit
            it[TimerPaused] = (endsAt - now()).coerceAtLeast(0)
            it.remove(TimerEndsAt)
        }
    }

    suspend fun resumeTimer() {
        store.edit {
            val left = it[TimerPaused] ?: return@edit
            it[TimerEndsAt] = now() + left
            it.remove(TimerPaused)
        }
    }

    /** Removes this duel's timer, running or paused; the chosen length stays for the next Play. */
    suspend fun removeTimer() { store.edit { it.removeTimer() } }

    /** Totals, history and timer; settings stay (SPEC §2). */
    suspend fun clearAll() {
        val prefs = store.edit {
            it.remove(KeyP1); it.remove(KeyP2); it.remove(HistoryP1); it.remove(HistoryP2)
            it.remove(Duels); it.removeTimer()
        }
        onChange(prefs.toLifePoints())
    }

    private fun MutablePreferences.removeTimer() { remove(TimerEndsAt); remove(TimerPaused) }

    private val Preferences.starting get() = this[StartingLifePoints] ?: INITIAL_LIFE_POINTS

    private fun Preferences.toLifePoints() =
        LifePoints(this[KeyP1] ?: starting, this[KeyP2] ?: starting)

    private fun Preferences.history(player: Player): List<Int> =
        this[player.historyKey]?.split(',')?.filter(String::isNotEmpty)?.map(String::toInt).orEmpty()

    private fun MutablePreferences.setHistory(player: Player, list: List<Int>) {
        if (list.isEmpty()) remove(player.historyKey) else this[player.historyKey] = list.joinToString(",")
    }
}

fun GameRepository(context: Context): GameRepository {
    val appContext = context.applicationContext
    return GameRepository(appContext.dataStore) { refreshSurfaces(appContext) }
}
