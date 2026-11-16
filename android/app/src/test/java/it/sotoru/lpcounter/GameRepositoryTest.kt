package it.sotoru.lpcounter

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import it.sotoru.lpcounter.data.Duel
import it.sotoru.lpcounter.data.GameRepository
import it.sotoru.lpcounter.data.INITIAL_LIFE_POINTS
import it.sotoru.lpcounter.data.LifePoints
import it.sotoru.lpcounter.data.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GameRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notified = mutableListOf<LifePoints>()
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: GameRepository
    private var clock = 1_000_000L

    @Before fun setUp() {
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "game.preferences_pb") }
        repo = GameRepository(store, now = { clock }) { notified += it }
    }

    @After fun tearDown() = scope.cancel()

    private fun game() = runBlocking { repo.game.first() }
    private fun history(p: Player) = game().history.getValue(p)

    @Test fun startsAtInitialWithEmptyHistory() {
        assertEquals(LifePoints(), game().lifePoints)
        assertEquals(emptyList<Int>(), history(Player.One))
    }

    @Test fun updateRecordsSignedChanges() = runBlocking {
        repo.update(Player.One) { it - 500 }
        repo.update(Player.One) { it + 1000 }
        assertEquals(listOf(-500, 1000), history(Player.One))
        assertEquals(INITIAL_LIFE_POINTS + 500, game().lifePoints.one)
        assertEquals(INITIAL_LIFE_POINTS, game().lifePoints.two)
    }

    @Test fun updateClampsAtZeroAndRecordsAppliedChange() = runBlocking {
        repo.update(Player.Two) { it - 9000 }
        assertEquals(0, game().lifePoints.two)
        assertEquals(listOf(-INITIAL_LIFE_POINTS), history(Player.Two))
    }

    @Test fun noOpUpdateIsNotRecorded() = runBlocking {
        repo.update(Player.One) { it }
        assertEquals(emptyList<Int>(), history(Player.One))
    }

    @Test fun undoIsRelativeToCurrentTotal() = runBlocking {
        repo.update(Player.One) { it - 500 }  // 7500, history [-500]
        // A change that bypasses the repository (no history entry) must survive the undo.
        store.edit { it[intPreferencesKey("lifePointsP1")] = 9000 }
        repo.undo(Player.One)
        assertEquals(9500, game().lifePoints.one)
        assertEquals(emptyList<Int>(), history(Player.One))
    }

    @Test fun undoWithEmptyHistoryDoesNothing() = runBlocking {
        repo.undo(Player.One)
        assertEquals(LifePoints(), game().lifePoints)
    }

    @Test fun resetPlayerClearsOnlyThatPlayer() = runBlocking {
        repo.update(Player.One) { it - 500 }
        repo.update(Player.Two) { it - 300 }
        repo.reset(Player.One)
        assertEquals(INITIAL_LIFE_POINTS, game().lifePoints.one)
        assertEquals(emptyList<Int>(), history(Player.One))
        assertEquals(listOf(-300), history(Player.Two))
    }

    @Test fun resetGameKeepsHistory() = runBlocking {
        repo.update(Player.One) { it - 500 }
        repo.reset()
        assertEquals(LifePoints(), game().lifePoints)
        assertEquals(listOf(-500), history(Player.One))
    }

    @Test fun historyIsBounded() = runBlocking {
        repeat(250) { repo.update(Player.One) { it + 1 } }
        assertEquals(200, history(Player.One).size)
    }

    @Test fun halveRoundsUp() = runBlocking {
        // Same transform as GameViewModel.halve (SPEC §2): 2425 → 1213, 1 stays 1.
        store.edit { it[intPreferencesKey("lifePointsP1")] = 2425 }
        repo.update(Player.One) { (it + 1) / 2 }
        assertEquals(1213, game().lifePoints.one)
        store.edit { it[intPreferencesKey("lifePointsP2")] = 1 }
        repo.update(Player.Two) { (it + 1) / 2 }
        assertEquals(1, game().lifePoints.two)
    }

    @Test fun startingLifePointsApplyNowOnlyIfTheDuelHasNotStarted() = runBlocking {
        repo.setStartingLifePoints(4000)
        assertEquals(LifePoints(4000, 4000), game().lifePoints)
        assertEquals(4000, game().startingLifePoints)

        repo.update(Player.One) { it - 500 }
        repo.setStartingLifePoints(INITIAL_LIFE_POINTS)
        assertEquals(3500, game().lifePoints.one)
        repo.reset(Player.Two)
        assertEquals(INITIAL_LIFE_POINTS, game().lifePoints.two)
        repo.reset()
        assertEquals(LifePoints(), game().lifePoints)
    }

    @Test fun duelIsOverWhenAPlayerReachesZero() = runBlocking {
        assertEquals(false, game().isOver)
        repo.update(Player.Two) { it - 9900 }
        assertEquals(true, game().isOver)
    }

    @Test fun everyWriteNotifiesSurfaces() = runBlocking {
        repo.update(Player.One) { it - 500 }
        repo.undo(Player.One)
        repo.reset()
        assertEquals(listOf(7500, 8000, 8000), notified.map { it.one })
    }

    @Test fun resetSavesPlayedDuelsNewestFirst() = runBlocking {
        repo.reset()  // untouched duel: not saved
        repo.update(Player.One) { it - 8000 }
        repo.reset()
        clock += 1
        repo.update(Player.Two) { it - 500 }
        repo.reset()
        assertEquals(listOf(Duel(clock, 8000, 7500), Duel(clock - 1, 0, 8000)), game().duels)
    }

    @Test fun startDuelStartsTimerOnlyWhenNoneIsRunning() = runBlocking {
        repo.startDuel()
        assertEquals(null, game().timerEndsAt)
        repo.setTimerMinutes(50)
        repo.startDuel()
        val endsAt = clock + 50 * 60_000L
        assertEquals(endsAt, game().timerEndsAt)
        clock += 60_000
        repo.startDuel()
        assertEquals(endsAt, game().timerEndsAt)
        clock = endsAt
        repo.startDuel()
        assertEquals(clock + 50 * 60_000L, game().timerEndsAt)
        repo.setTimerMinutes(0)
        repo.startDuel()
        assertEquals(null, game().timerEndsAt)
    }

    @Test fun clearAllKeepsStartingLifePoints() = runBlocking {
        repo.setStartingLifePoints(4000)
        repo.setTimerMinutes(50)
        repo.update(Player.One) { it - 500 }
        repo.reset()
        repo.startDuel()
        repo.clearAll()
        val g = game()
        assertEquals(LifePoints(4000, 4000), g.lifePoints)
        assertEquals(emptyList<Int>(), g.history.getValue(Player.One))
        assertEquals(emptyList<Duel>(), g.duels)
        assertEquals(null, g.timerEndsAt)
        assertEquals(50, g.timerMinutes)
    }
}
