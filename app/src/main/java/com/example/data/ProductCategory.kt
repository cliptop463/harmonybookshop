package com.example.data

import androidx.room.TypeConverter

/**
 * Enumeration of product categories stocked by Harmony Bookshop.
 */
enum class ProductCategory(val displayName: String) {
    PRINTER("Printer"),
    TEXTBOOK("Textbook"),
    EXERCISE_BOOK("Exercise Book"),
    TYPING_SHEET("Typing Sheet"),
    PEN("Pen"),
    PENCIL("Pencil"),
    FOLDER("Folder"),
    INK("Ink"),
    STATIONARY("Stationary"),
    OTHER("Other");

    companion object {
        val DEFAULT: ProductCategory = STATIONARY

        fun fromRaw(raw: String?): ProductCategory {
            val normalized = raw?.trim().orEmpty()
            if (normalized.isEmpty()) return DEFAULT
            return entries.firstOrNull { category ->
                category.name.equals(normalized, ignoreCase = true) ||
                    category.displayName.equals(normalized, ignoreCase = true) ||
                    category.name.replace('_', ' ').equals(normalized, ignoreCase = true)
            } ?: when (normalized.lowercase()) {
                "stationery" -> STATIONARY
                "exercise books" -> EXERCISE_BOOK
                "textbooks" -> TEXTBOOK
                "typing sheets" -> TYPING_SHEET
                "printers" -> PRINTER
                "pens" -> PEN
                "pencils" -> PENCIL
                "folders" -> FOLDER
                else -> OTHER
            }
        }
    }
}

class ProductTypeConverters {
    @TypeConverter
    fun fromCategory(category: ProductCategory): String = category.name

    @TypeConverter
    fun toCategory(raw: String?): ProductCategory = ProductCategory.fromRaw(raw)
}
