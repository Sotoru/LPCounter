package it.sotoru.lpcounter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.sotoru.lpcounter.ui.led.LedPalette
import it.sotoru.lpcounter.ui.led.PixelStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Seconds without a touch before the duel screen dims (SPEC §5); 0 never. */
val DIM_AFTER_OPTIONS = listOf(0, 30, 60, 120, 300)

/** App preferences (SPEC §5). The starting LP lives in [GameRepository] because resets and widgets need it. */
data class Settings(
    val confirmReset: Boolean = true,
    val autoEnd: Boolean = true,
    val haptics: Boolean = true,
    val dimAfter: Int = 60,
    val palette: LedPalette = LedPalette.Night,
    val pixels: PixelStyle = PixelStyle.Round,
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

private val ConfirmReset = booleanPreferencesKey("confirmReset")
private val AutoEnd = booleanPreferencesKey("autoEndDuel")
private val Haptics = booleanPreferencesKey("haptics")
private val DimAfter = intPreferencesKey("dimAfter")
private val Palette = stringPreferencesKey("ledPalette")
private val Pixels = stringPreferencesKey("pixelStyle")

class SettingsRepository(
    private val store: DataStore<Preferences>,
    // Widgets and the notification draw in the chosen colour and dot shape, so they are redrawn on a change.
    private val onLookChange: suspend () -> Unit = {},
) {
    val settings: Flow<Settings> = store.data.map {
        Settings(
            confirmReset = it[ConfirmReset] ?: true,
            autoEnd = it[AutoEnd] ?: true,
            haptics = it[Haptics] ?: true,
            dimAfter = it[DimAfter] ?: 60,
            palette = LedPalette.entries.firstOrNull { p -> p.name == it[Palette] } ?: LedPalette.Night,
            pixels = PixelStyle.entries.firstOrNull { p -> p.name == it[Pixels] } ?: PixelStyle.Round,
        )
    }

    suspend fun setConfirmReset(value: Boolean) { store.edit { it[ConfirmReset] = value } }
    suspend fun setAutoEnd(value: Boolean) { store.edit { it[AutoEnd] = value } }
    suspend fun setHaptics(value: Boolean) { store.edit { it[Haptics] = value } }
    suspend fun setDimAfter(value: Int) { store.edit { it[DimAfter] = value } }
    suspend fun setPalette(value: LedPalette) { store.edit { it[Palette] = value.name }; onLookChange() }
    suspend fun setPixels(value: PixelStyle) { store.edit { it[Pixels] = value.name }; onLookChange() }
}

fun SettingsRepository(context: Context, onLookChange: suspend () -> Unit = {}) =
    SettingsRepository(context.applicationContext.settingsStore, onLookChange)
