package it.sotoru.lpcounter.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.sotoru.lpcounter.LPCounterApp
import it.sotoru.lpcounter.data.GameRepository
import it.sotoru.lpcounter.data.GameState
import it.sotoru.lpcounter.data.Player
import it.sotoru.lpcounter.data.Settings
import it.sotoru.lpcounter.data.SettingsRepository
import it.sotoru.lpcounter.ui.led.LedPalette
import it.sotoru.lpcounter.ui.led.PixelStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    private val _resets = MutableStateFlow(0)
    /** Bumped by every game reset, so the totals can tell it from any other change and play its effect. */
    val resets: StateFlow<Int> = _resets.asStateFlow()

    fun changeBy(player: Player, delta: Int) = viewModelScope.launch {
        repository.update(player) { it + delta }
    }

    // Rounds up, as the official rulings do (2425 → 1213).
    fun halve(player: Player) = viewModelScope.launch { repository.update(player) { (it + 1) / 2 } }

    fun undo(player: Player) = viewModelScope.launch { repository.undo(player) }

    /** A new duel: both totals back to the start, both histories cleared (SPEC §2). */
    fun resetGame() = viewModelScope.launch {
        _resets.update { it + 1 }
        repository.reset()
    }

    fun setStartingLifePoints(value: Int) = viewModelScope.launch { repository.setStartingLifePoints(value) }

    fun setTimerMinutes(value: Int) = viewModelScope.launch { repository.setTimerMinutes(value) }
    fun startDuel() = viewModelScope.launch { repository.startDuel() }
    fun pauseTimer() = viewModelScope.launch { repository.pauseTimer() }
    fun resumeTimer() = viewModelScope.launch { repository.resumeTimer() }
    fun removeTimer() = viewModelScope.launch { repository.removeTimer() }
    fun clearAll() = viewModelScope.launch { repository.clearAll() }

    fun setConfirmReset(value: Boolean) = viewModelScope.launch { settingsRepository.setConfirmReset(value) }
    fun setAutoEnd(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoEnd(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { settingsRepository.setHaptics(value) }
    fun setDimAfter(value: Int) = viewModelScope.launch { settingsRepository.setDimAfter(value) }
    fun setPalette(value: LedPalette) = viewModelScope.launch { settingsRepository.setPalette(value) }
    fun setPixels(value: PixelStyle) = viewModelScope.launch { settingsRepository.setPixels(value) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LPCounterApp
                GameViewModel(app.repository, app.settings)
            }
        }
    }
}
