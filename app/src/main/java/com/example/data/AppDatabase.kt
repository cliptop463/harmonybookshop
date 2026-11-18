package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Offline SQLite database holding the sales product catalog.
 */
@Database(entities = [Product::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "margindesk_inventory.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}

/**
 * Default sample catalog seeded once on first launch so sales teams can immediately test
 * price lookup, stock adjustments, low-stock badges, and margin tracking.
 */
object SeedCatalog {
    fun defaultProducts(): List<Product> = listOf(
        Product(
            name = "Wireless Barcode Scanner Pro",
            skuOrCode = "POS-104",
            quantity = 18,
            costPrice = 42.0,
            sellingPrice = 75.0,
        ),
        Product(
            name = "Thermal Receipt Printer 80mm",
            skuOrCode = "PRT-208",
            quantity = 4,
            costPrice = 68.0,
            sellingPrice = 115.0,
        ),
        Product(
            name = "USB-C Multiport Hub 7-in-1",
            skuOrCode = "ACC-312",
            quantity = 32,
            costPrice = 14.0,
            sellingPrice = 29.0,
        ),
        Product(
            name = "Ergonomic Aluminum Laptop Stand",
            skuOrCode = "OFC-119",
            quantity = 3,
            costPrice = 22.0,
            sellingPrice = 38.0,
        ),
        Product(
            name = "Heavy-Duty Cash Drawer Steel",
            skuOrCode = "POS-405",
            quantity = 9,
            costPrice = 55.0,
            sellingPrice = 89.0,
        ),
        Product(
            name = "NFC Countertop Card Reader",
            skuOrCode = "PAY-501",
            quantity = 0,
            costPrice = 48.0,
            sellingPrice = 79.0,
        ),
        Product(
            name = "Thermal Paper Roll Pack (20x)",
            skuOrCode = "SUP-088",
            quantity = 45,
            costPrice = 12.0,
            sellingPrice = 18.0,
        ),
        Product(
            name = "Clearance Display Cable Bundle",
            skuOrCode = "CLR-009",
            quantity = 5,
            costPrice = 19.0,
            sellingPrice = 16.0,
        ),
    )
}
