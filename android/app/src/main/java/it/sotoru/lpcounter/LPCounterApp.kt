package it.sotoru.lpcounter

import android.app.Application
import android.content.Context
import it.sotoru.lpcounter.data.GameRepository
import it.sotoru.lpcounter.data.SettingsRepository
import it.sotoru.lpcounter.widget.refreshSurfaces

class LPCounterApp : Application() {
    val repository by lazy { GameRepository(this) }
    val settings by lazy { SettingsRepository(this) { refreshSurfaces(this) } }
}

val Context.gameRepository: GameRepository
    get() = (applicationContext as LPCounterApp).repository

val Context.settingsRepository: SettingsRepository
    get() = (applicationContext as LPCounterApp).settings
