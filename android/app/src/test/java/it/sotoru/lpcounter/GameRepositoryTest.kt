package it.sotoru.lpcounter

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    @Before fun setUp() {
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "game.preferences_pb") }
        repo = GameRepository(store) { notified += it }
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

    @Test fun everyWriteNotifiesSurfaces() = runBlocking {
        repo.update(Player.One) { it - 500 }
        repo.undo(Player.One)
        repo.reset()
        assertEquals(listOf(7500, 8000, 8000), notified.map { it.one })
    }
}
