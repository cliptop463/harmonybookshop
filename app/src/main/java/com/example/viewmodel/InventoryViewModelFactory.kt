package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.repository.ProductRepository
import com.example.repository.SettingsRepository

/**
 * Manual DI ViewModelProvider.Factory for [InventoryViewModel].
 */
class InventoryViewModelFactory(
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            return InventoryViewModel(
                productRepository = productRepository,
                settingsRepository = settingsRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
