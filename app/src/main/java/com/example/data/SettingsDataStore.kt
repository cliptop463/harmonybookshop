package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "margindesk_settings"
)

/**
 * User-configurable display and inventory threshold preferences.
 */
data class AppSettings(
    val currencySymbol: String = "XAF",
    val decimalPlaces: Int = 0,
    val lowStockThreshold: Int = 5,
    val dynamicColor: Boolean = false,
)

/**
 * Offline DataStore wrapper for app preferences and one-time seed tracking.
 */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val LOW_STOCK_THRESHOLD = intPreferencesKey("low_stock_threshold")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HAS_SEEDED = booleanPreferencesKey("has_seeded")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppSettings(
                currencySymbol = prefs[Keys.CURRENCY_SYMBOL]?.takeIf { it.isNotBlank() } ?: "XAF",
                decimalPlaces = (prefs[Keys.DECIMAL_PLACES] ?: 0).coerceIn(0, 4),
                lowStockThreshold = (prefs[Keys.LOW_STOCK_THRESHOLD] ?: 5).coerceAtLeast(0),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            )
        }

    suspend fun hasSeeded(): Boolean {
        val prefs = context.settingsDataStore.data
            .catch { emit(emptyPreferences()) }
            .first()
        return prefs[Keys.HAS_SEEDED] ?: false
    }

    suspend fun markSeeded() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED] = true
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CURRENCY_SYMBOL] = settings.currencySymbol.trim().ifEmpty { "XAF" }
            prefs[Keys.DECIMAL_PLACES] = settings.decimalPlaces.coerceIn(0, 4)
            prefs[Keys.LOW_STOCK_THRESHOLD] = settings.lowStockThreshold.coerceAtLeast(0)
            prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColor
        }
    }
}
