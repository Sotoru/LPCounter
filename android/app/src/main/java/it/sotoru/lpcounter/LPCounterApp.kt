package it.sotoru.lpcounter

import android.app.Application
import android.content.Context
import it.sotoru.lpcounter.data.GameRepository
import it.sotoru.lpcounter.data.SettingsRepository

class LPCounterApp : Application() {
    val repository by lazy { GameRepository(this) }
    val settings by lazy { SettingsRepository(this) }
}

val Context.gameRepository: GameRepository
    get() = (applicationContext as LPCounterApp).repository
