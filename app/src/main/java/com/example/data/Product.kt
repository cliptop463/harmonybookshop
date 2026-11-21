package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a sellable product in the offline catalog.
 *
 * [profit] and [profitMargin] are intentionally computed properties rather than persisted columns
 * so they always reflect the exact current [costPrice] and [sellingPrice].
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: ProductCategory = ProductCategory.DEFAULT,
    val quantity: Int,
    val costPrice: Double,
    val sellingPrice: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** Net profit per unit: sellingPrice - costPrice. */
    val profit: Double
        get() = sellingPrice - costPrice

    /** Gross profit margin percentage based on selling price; returns 0.0 when sellingPrice is 0. */
    val profitMargin: Double
        get() = if (sellingPrice == 0.0) 0.0 else (profit / sellingPrice) * 100.0
}
