package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Product
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
    fun `room like search filters by name and code and clamps quantity at zero`() = runBlocking {
        val dao = database.productDao()
        val id1 = dao.insertProduct(
            Product(
                name = "Barcode Scanner",
                skuOrCode = "POS-100",
                quantity = 1,
                costPrice = 30.0,
                sellingPrice = 50.0,
            ),
        )
        dao.insertProduct(
            Product(
                name = "Thermal Paper",
                skuOrCode = null,
                quantity = 10,
                costPrice = 5.0,
                sellingPrice = 10.0,
            ),
        )

        val byCode = dao.searchProducts("POS").first()
        assertEquals(1, byCode.size)
        assertEquals("Barcode Scanner", byCode.first().name)

        val allWithBlankQuery = dao.searchProducts("").first()
        assertEquals(2, allWithBlankQuery.size)

        dao.adjustQuantity(id1, -1)
        dao.adjustQuantity(id1, -1) // should stay clamped at 0
        val updated = dao.searchProducts("Barcode").first().first()
        assertEquals(0, updated.quantity)
    }
}
