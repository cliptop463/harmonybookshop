package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Offline SQLite database holding the Harmony Bookshop product catalog.
 */
@Database(entities = [Product::class], version = 2, exportSchema = false)
@TypeConverters(ProductTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `products_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `costPrice` REAL NOT NULL,
                        `sellingPrice` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `products_new` (id, name, category, quantity, costPrice, sellingPrice, createdAt, updatedAt)
                    SELECT id, name, 'STATIONARY', quantity, costPrice, sellingPrice,
                           CAST(strftime('%s','now') AS INTEGER) * 1000,
                           CAST(strftime('%s','now') AS INTEGER) * 1000
                    FROM `products`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `products`")
                db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "margindesk_inventory.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

/**
 * Default Harmony Bookshop sample catalog seeded once on first launch.
 */
object SeedCatalog {
    fun defaultProducts(): List<Product> {
        val baseTime = System.currentTimeMillis()
        return listOf(
            Product(
                name = "Oxford English Dictionary (School Ed.)",
                category = ProductCategory.TEXTBOOK,
                quantity = 14,
                costPrice = 3200.0,
                sellingPrice = 4500.0,
                createdAt = baseTime - 70_000L,
                updatedAt = baseTime - 70_000L,
            ),
            Product(
                name = "200-Page Hardcover Exercise Book",
                category = ProductCategory.EXERCISE_BOOK,
                quantity = 48,
                costPrice = 350.0,
                sellingPrice = 500.0,
                createdAt = baseTime - 60_000L,
                updatedAt = baseTime - 60_000L,
            ),
            Product(
                name = "A4 Typing Sheet Ream (500 Sheets)",
                category = ProductCategory.TYPING_SHEET,
                quantity = 12,
                costPrice = 2800.0,
                sellingPrice = 3500.0,
                createdAt = baseTime - 50_000L,
                updatedAt = baseTime - 50_000L,
            ),
            Product(
                name = "Bic Cristal Blue Ballpoint Pen (Box 50)",
                category = ProductCategory.PEN,
                quantity = 4,
                costPrice = 3800.0,
                sellingPrice = 5000.0,
                createdAt = baseTime - 40_000L,
                updatedAt = baseTime - 40_000L,
            ),
            Product(
                name = "HB Graphite Drawing Pencil (Pack 12)",
                category = ProductCategory.PENCIL,
                quantity = 22,
                costPrice = 700.0,
                sellingPrice = 1000.0,
                createdAt = baseTime - 30_000L,
                updatedAt = baseTime - 30_000L,
            ),
            Product(
                name = "Plastic Lever Arch Box File Folder",
                category = ProductCategory.FOLDER,
                quantity = 3,
                costPrice = 1100.0,
                sellingPrice = 1500.0,
                createdAt = baseTime - 20_000L,
                updatedAt = baseTime - 20_000L,
            ),
            Product(
                name = "Epson EcoTank Black Ink Bottle 70ml",
                category = ProductCategory.INK,
                quantity = 0,
                costPrice = 4200.0,
                sellingPrice = 6000.0,
                createdAt = baseTime - 10_000L,
                updatedAt = baseTime - 10_000L,
            ),
            Product(
                name = "Compact Thermal Receipt Printer 80mm",
                category = ProductCategory.PRINTER,
                quantity = 5,
                costPrice = 38000.0,
                sellingPrice = 35000.0,
                createdAt = baseTime,
                updatedAt = baseTime,
            ),
        )
    }
}
