package com.example

import com.example.data.Product
import com.example.data.ProductCategory
import com.example.util.CurrencyFormatter
import com.example.util.CsvHandler
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductAndCsvUnitTest {

    @Test
    fun `product computed profit and margin handle normal and zero selling price`() {
        val profitable = Product(
            name = "Oxford Textbook",
            category = ProductCategory.TEXTBOOK,
            quantity = 4,
            costPrice = 3000.0,
            sellingPrice = 4500.0,
        )
        assertEquals(1500.0, profitable.profit, 0.001)
        assertEquals(33.333, profitable.profitMargin, 0.01)

        val zeroSelling = Product(
            name = "Promo Pen",
            category = ProductCategory.PEN,
            quantity = 2,
            costPrice = 100.0,
            sellingPrice = 0.0,
        )
        assertEquals(-100.0, zeroSelling.profit, 0.001)
        assertEquals(0.0, zeroSelling.profitMargin, 0.001)

        // Default category is STATIONARY
        val defaultCat = Product(
            name = "Stapler",
            quantity = 5,
            costPrice = 500.0,
            sellingPrice = 800.0,
        )
        assertEquals(ProductCategory.STATIONARY, defaultCat.category)
    }

    @Test
    fun `product category parser resolves display names and defaults to stationary when blank`() {
        assertEquals(ProductCategory.STATIONARY, ProductCategory.fromRaw(""))
        assertEquals(ProductCategory.STATIONARY, ProductCategory.fromRaw(null))
        assertEquals(ProductCategory.EXERCISE_BOOK, ProductCategory.fromRaw("Exercise Book"))
        assertEquals(ProductCategory.TYPING_SHEET, ProductCategory.fromRaw("typing sheet"))
        assertEquals(ProductCategory.PRINTER, ProductCategory.fromRaw("Printer"))
        assertEquals(ProductCategory.OTHER, ProductCategory.fromRaw("Unlisted"))
    }

    @Test
    fun `currency formatter respects symbol and decimal places`() {
        assertEquals("$1,250", CurrencyFormatter.formatPrice(1250.4, "$", 0))
        assertEquals("XAF 25,500", CurrencyFormatter.formatPrice(25500.0, "XAF", 0))
        assertEquals("€49.95", CurrencyFormatter.formatPrice(49.95, "€", 2))
        assertEquals("-XAF 150", CurrencyFormatter.formatSignedProfit(-150.0, "XAF", 0))
        assertEquals("+40.0%", CurrencyFormatter.formatMarginPercent(40.0))
    }

    @Test
    fun `csv handler exports and validates imported rows with categories while reporting skipped ones`() {
        val products = listOf(
            Product(
                name = "Exercise Book, 200p",
                category = ProductCategory.EXERCISE_BOOK,
                quantity = 5,
                costPrice = 350.0,
                sellingPrice = 500.0,
            ),
            Product(
                name = "Typing Sheet Ream",
                category = ProductCategory.TYPING_SHEET,
                quantity = 20,
                costPrice = 2800.0,
                sellingPrice = 3500.0,
            ),
        )
        val csv = CsvHandler.exportToCsv(products)
        val roundTrip = CsvHandler.parseCsv(csv)
        assertEquals(2, roundTrip.importedCount)
        assertEquals(0, roundTrip.skippedCount)
        assertEquals("Exercise Book, 200p", roundTrip.validProducts[0].name)
        assertEquals(ProductCategory.EXERCISE_BOOK, roundTrip.validProducts[0].category)

        val mixedCsv = """
            name,category,quantity,costPrice,sellingPrice
            Valid Item,Folder,12,1000,1500
            ,Pen,5,100,200
            Bad Qty,Pencil,2.5,100,200
            Negative Cost,Ink,3,-5,200
            Too Few Cols,Printer,1
        """.trimIndent()

        val result = CsvHandler.parseCsv(mixedCsv)
        assertEquals(1, result.importedCount)
        assertEquals(4, result.skippedCount)
        assertEquals("Valid Item", result.validProducts.first().name)
        assertEquals(ProductCategory.FOLDER, result.validProducts.first().category)
    }
}
