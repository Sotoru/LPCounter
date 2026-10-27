package it.sotoru.lpcounter

import android.app.Application
import android.content.Context
import it.sotoru.lpcounter.data.GameRepository

class LPCounterApp : Application() {
    val repository by lazy { GameRepository(this) }
}

val Context.gameRepository: GameRepository
    get() = (applicationContext as LPCounterApp).repository
