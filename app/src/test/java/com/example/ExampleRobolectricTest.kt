package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.viewmodel.OrderFormState
import com.example.viewmodel.OrderLineItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Harmony Bookshop Management", appName)
    }

    @Test
    fun `room like search filters by name and category and updates modified timestamp`() = runBlocking {
        val dao = database.productDao()
        val id1 = dao.insertProduct(
            Product(
                name = "200-Page Notebook",
                category = ProductCategory.EXERCISE_BOOK,
                quantity = 1,
                costPrice = 350.0,
                sellingPrice = 500.0,
                createdAt = 1_000L,
                updatedAt = 1_000L,
            ),
        )
        dao.insertProduct(
            Product(
                name = "A4 Paper Ream",
                category = ProductCategory.TYPING_SHEET,
                quantity = 10,
                costPrice = 2800.0,
                sellingPrice = 3500.0,
                createdAt = 1_500L,
                updatedAt = 1_500L,
            ),
        )

        val byCategory = dao.searchProducts("Exercise Book").first()
        assertEquals(1, byCategory.size)
        assertEquals("200-Page Notebook", byCategory.first().name)

        val allWithBlankQuery = dao.searchProducts("").first()
        assertEquals(2, allWithBlankQuery.size)

        dao.adjustQuantity(id1, -1, updatedAt = 2_000L)
        dao.adjustQuantity(id1, -1, updatedAt = 3_000L) // stays clamped at 0
        val updated = dao.searchProducts("Notebook").first().first()
        assertEquals(0, updated.quantity)
        assertEquals(3_000L, updated.updatedAt)

        // Most recently modified item appears first under observeAllProducts()
        val ordered = dao.observeAllProducts().first()
        assertEquals("200-Page Notebook", ordered.first().name)
    }

    @Test
    fun `order state calculates xaf totals and unit counts accurately`() {
        val order = OrderFormState(
            customerName = "St. Augustine College",
            customerAddress = "Kumbo Main Road",
            items = listOf(
                OrderLineItem(
                    lineId = 1L,
                    name = "Oxford Dictionary",
                    category = ProductCategory.TEXTBOOK,
                    quantity = 2,
                    unitPriceXaf = 4500.0,
                ),
                OrderLineItem(
                    lineId = 2L,
                    name = "A4 Typing Sheet Ream",
                    category = ProductCategory.TYPING_SHEET,
                    quantity = 3,
                    unitPriceXaf = 3500.0,
                ),
            ),
        )

        assertEquals(5, order.totalUnits)
        assertEquals(19500.0, order.totalXaf, 0.001)
    }
}
