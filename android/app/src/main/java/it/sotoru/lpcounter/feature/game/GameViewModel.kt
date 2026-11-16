package it.sotoru.lpcounter.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.sotoru.lpcounter.LPCounterApp
import it.sotoru.lpcounter.data.GameRepository
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.AppTheme
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.data.Settings
import it.sotoru.lpcounter.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Null until the first DataStore read, so the UI never flashes a bogus 8000. */
    val state: StateFlow<GameState?> =
        repository.game.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<Settings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun changeBy(player: Player, delta: Int) = viewModelScope.launch {
        repository.update(player) { it + delta }
    }

    // Rounds up, as the official rulings do (2425 → 1213).
    fun halve(player: Player) = viewModelScope.launch { repository.update(player) { (it + 1) / 2 } }

    fun undo(player: Player) = viewModelScope.launch { repository.undo(player) }

    fun resetPlayer(player: Player) = viewModelScope.launch { repository.reset(player) }

    // Like iOS, the per-player history is intentionally left untouched.
    fun resetGame() = viewModelScope.launch { repository.reset() }

    fun setStartingLifePoints(value: Int) = viewModelScope.launch { repository.setStartingLifePoints(value) }

    fun setTimerMinutes(value: Int) = viewModelScope.launch { repository.setTimerMinutes(value) }
    fun startDuel() = viewModelScope.launch { repository.startDuel() }
    fun clearAll() = viewModelScope.launch { repository.clearAll() }

    fun setConfirmReset(value: Boolean) = viewModelScope.launch { settingsRepository.setConfirmReset(value) }
    fun setAutoEnd(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoEnd(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { settingsRepository.setHaptics(value) }
    fun setTheme(value: AppTheme) = viewModelScope.launch { settingsRepository.setTheme(value) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LPCounterApp
                GameViewModel(app.repository, app.settings)
            }
        }
    }
}
