package com.example

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.SettingsDataStore
import com.example.repository.ProductRepository
import com.example.repository.SettingsRepository
import com.example.viewmodel.InventoryViewModelFactory

/**
 * Manual Dependency Injection container scoped to the Application lifecycle.
 * Avoids Hilt overhead while keeping data, repository, and ViewModel layers decoupled.
 */
class AppContainer(context: Context) {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepository(
            productDao = database.productDao(),
            settingsDataStore = settingsDataStore,
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(settingsDataStore = settingsDataStore)
    }

    val inventoryViewModelFactory: InventoryViewModelFactory by lazy {
        InventoryViewModelFactory(
            productRepository = productRepository,
            settingsRepository = settingsRepository,
        )
    }
}
