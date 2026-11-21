package com.example.repository

import com.example.data.Product
import com.example.data.ProductDao
import com.example.data.SeedCatalog
import com.example.data.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for offline product inventory operations.
 * Abstracts Room DAO queries and first-launch seeding from the ViewModel layer.
 */
class ProductRepository(
    private val productDao: ProductDao,
    private val settingsDataStore: SettingsDataStore,
) {
    private val seedMutex = Mutex()

    fun observeAllProducts(): Flow<List<Product>> = productDao.observeAllProducts()

    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.searchProducts(query.trim())

    /**
     * Seeds sample bookshop products once on the very first app launch.
     * Protected by a Mutex and DataStore flag so deleting all items later never re-seeds them.
     */
    suspend fun seedInitialProductsIfNeeded() {
        seedMutex.withLock {
            if (settingsDataStore.hasSeeded()) return
            if (productDao.getProductCount() == 0) {
                productDao.insertProducts(SeedCatalog.defaultProducts())
            }
            settingsDataStore.markSeeded()
        }
    }

    suspend fun saveProduct(product: Product) {
        val now = System.currentTimeMillis()
        if (product.id == 0L) {
            productDao.insertProduct(
                product.copy(
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            val existing = productDao.getProductById(product.id)
            productDao.updateProduct(
                product.copy(
                    createdAt = existing?.createdAt ?: product.createdAt,
                    updatedAt = now,
                )
            )
        }
    }

    suspend fun incrementQuantity(productId: Long) {
        productDao.adjustQuantity(productId, delta = 1)
    }

    suspend fun decrementQuantity(productId: Long) {
        productDao.adjustQuantity(productId, delta = -1)
    }

    suspend fun deductStock(productId: Long, quantitySold: Int) {
        if (quantitySold <= 0) return
        productDao.adjustQuantity(productId, delta = -quantitySold)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun restoreProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun getAllProductsForExport(): List<Product> = productDao.getAllProductsOnce()

    suspend fun importProducts(products: List<Product>) {
        if (products.isNotEmpty()) {
            productDao.insertProducts(products)
        }
    }
}
