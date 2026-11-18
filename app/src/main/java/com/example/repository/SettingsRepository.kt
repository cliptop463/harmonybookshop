package com.example.repository

import com.example.data.AppSettings
import com.example.data.SettingsDataStore
import kotlinx.coroutines.flow.Flow

/**
 * Repository exposing user preferences (currency, decimals, low-stock threshold, dynamic color).
 */
class SettingsRepository(
    private val settingsDataStore: SettingsDataStore,
) {
    val settings: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun updateSettings(settings: AppSettings) {
        settingsDataStore.updateSettings(settings)
    }
}
