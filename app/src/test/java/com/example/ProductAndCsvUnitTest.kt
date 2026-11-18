package com.example

import com.example.data.Product
import com.example.util.CurrencyFormatter
import com.example.util.CsvHandler
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductAndCsvUnitTest {

    @Test
    fun `product computed profit and margin handle normal and zero selling price`() {
        val profitable = Product(
            name = "Scanner",
            skuOrCode = "POS-1",
            quantity = 4,
            costPrice = 40.0,
            sellingPrice = 80.0,
        )
        assertEquals(40.0, profitable.profit, 0.001)
        assertEquals(50.0, profitable.profitMargin, 0.001)

        val zeroSelling = Product(
            name = "Promo Item",
            skuOrCode = null,
            quantity = 2,
            costPrice = 10.0,
            sellingPrice = 0.0,
        )
        assertEquals(-10.0, zeroSelling.profit, 0.001)
        assertEquals(0.0, zeroSelling.profitMargin, 0.001)
    }

    @Test
    fun `currency formatter respects symbol and decimal places`() {
        assertEquals("$1,250", CurrencyFormatter.formatPrice(1250.4, "$", 0))
        assertEquals("XAF 25,500", CurrencyFormatter.formatPrice(25500.0, "XAF", 0))
        assertEquals("€49.95", CurrencyFormatter.formatPrice(49.95, "€", 2))
        assertEquals("-$15", CurrencyFormatter.formatSignedProfit(-15.0, "$", 0))
        assertEquals("+40.0%", CurrencyFormatter.formatMarginPercent(40.0))
    }

    @Test
    fun `csv handler exports and validates imported rows while reporting skipped ones`() {
        val products = listOf(
            Product(name = "Scanner, Pro", skuOrCode = "POS-1", quantity = 5, costPrice = 40.0, sellingPrice = 75.0),
            Product(name = "Paper Roll", skuOrCode = null, quantity = 20, costPrice = 10.0, sellingPrice = 15.0),
        )
        val csv = CsvHandler.exportToCsv(products)
        val roundTrip = CsvHandler.parseCsv(csv)
        assertEquals(2, roundTrip.importedCount)
        assertEquals(0, roundTrip.skippedCount)
        assertEquals("Scanner, Pro", roundTrip.validProducts[0].name)

        val mixedCsv = """
            name,skuOrCode,quantity,costPrice,sellingPrice
            Valid Item,SKU-1,12,100,150
            ,SKU-2,5,10,20
            Bad Qty,SKU-3,2.5,10,20
            Negative Cost,SKU-4,3,-5,20
            Too Few Cols,SKU-5,1
        """.trimIndent()

        val result = CsvHandler.parseCsv(mixedCsv)
        assertEquals(1, result.importedCount)
        assertEquals(4, result.skippedCount)
        assertEquals("Valid Item", result.validProducts.first().name)
    }
}
