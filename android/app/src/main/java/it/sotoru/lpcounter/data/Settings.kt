package it.sotoru.lpcounter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme { System, Light, Dark }

/** App-only preferences (SPEC §5). The starting LP lives in [GameRepository] because resets and widgets need it. */
data class Settings(
    val confirmReset: Boolean = true,
    val autoEnd: Boolean = true,
    val haptics: Boolean = true,
    val theme: AppTheme = AppTheme.System,
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

private val ConfirmReset = booleanPreferencesKey("confirmReset")
private val AutoEnd = booleanPreferencesKey("autoEndDuel")
private val Haptics = booleanPreferencesKey("haptics")
private val Theme = stringPreferencesKey("theme")

class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<Settings> = store.data.map {
        Settings(
            confirmReset = it[ConfirmReset] ?: true,
            autoEnd = it[AutoEnd] ?: true,
            haptics = it[Haptics] ?: true,
            theme = it[Theme]?.let { name -> AppTheme.entries.firstOrNull { t -> t.name == name } } ?: AppTheme.System,
        )
    }

    suspend fun setConfirmReset(value: Boolean) { store.edit { it[ConfirmReset] = value } }
    suspend fun setAutoEnd(value: Boolean) { store.edit { it[AutoEnd] = value } }
    suspend fun setHaptics(value: Boolean) { store.edit { it[Haptics] = value } }
    suspend fun setTheme(value: AppTheme) { store.edit { it[Theme] = value.name } }
}

fun SettingsRepository(context: Context) = SettingsRepository(context.applicationContext.settingsStore)
