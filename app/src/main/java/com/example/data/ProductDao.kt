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

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun observeAllProducts(): Flow<List<Product>>

    /**
     * Real-time LIKE query matching product name or optional SKU/code.
     * Uses IFNULL so products without a code still match when query is blank.
     */
    @Query(
        """
        SELECT * FROM products
        WHERE :query = ''
           OR name LIKE '%' || :query || '%'
           OR IFNULL(skuOrCode, '') LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllProductsOnce(): List<Product>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    /**
     * Atomically adjusts stock quantity while clamping at 0 so rapid taps never yield negative stock.
     */
    @Query("UPDATE products SET quantity = MAX(0, quantity + :delta) WHERE id = :productId")
    suspend fun adjustQuantity(productId: Long, delta: Int)

    @Delete
    suspend fun deleteProduct(product: Product)
}
