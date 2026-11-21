package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Product] operations backed by Room SQLite.
 */
@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY updatedAt DESC, id DESC")
    fun observeAllProducts(): Flow<List<Product>>

    /**
     * Real-time LIKE query matching product name or category display text.
     */
    @Query(
        """
        SELECT * FROM products
        WHERE :query = ''
           OR name LIKE '%' || :query || '%'
           OR REPLACE(category, '_', ' ') LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC, id DESC
        """
    )
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY updatedAt DESC, id DESC")
    suspend fun getAllProductsOnce(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    /**
     * Atomically adjusts stock quantity while clamping at 0 and refreshes [updatedAt]
     * so modified items reflect the latest change timestamp.
     */
    @Query("UPDATE products SET quantity = MAX(0, quantity + :delta), updatedAt = :updatedAt WHERE id = :productId")
    suspend fun adjustQuantity(
        productId: Long,
        delta: Int,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Delete
    suspend fun deleteProduct(product: Product)
}
