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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    /** Null until the first DataStore read, so the UI never flashes a bogus 8000. */
    val state: StateFlow<GameState?> =
        repository.game.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun changeBy(player: Player, delta: Int) = viewModelScope.launch {
        repository.update(player) { it + delta }
    }

    fun halve(player: Player) = viewModelScope.launch { repository.update(player) { it / 2 } }

    fun undo(player: Player) = viewModelScope.launch { repository.undo(player) }

    fun resetPlayer(player: Player) = viewModelScope.launch { repository.reset(player) }

    // Like iOS, the per-player history is intentionally left untouched.
    fun resetGame() = viewModelScope.launch { repository.reset() }

    companion object {
        val Factory = viewModelFactory {
            initializer { GameViewModel((this[APPLICATION_KEY] as LPCounterApp).repository) }
        }
    }
}
