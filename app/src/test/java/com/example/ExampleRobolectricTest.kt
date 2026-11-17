package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.OrderStatus
import com.example.model.ProductCategory
import com.example.model.StarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Buyzora`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Buyzora", appName)
    }

    @Test
    fun `starter catalog covers all 10 shopping categories`() {
        val catalogCategories = StarterCatalog.products.map { it.category }.toSet()
        ProductCategory.selectableCategories.forEach { category ->
            assertTrue(
                "Missing starter products for category ${category.displayName}",
                catalogCategories.contains(category.displayName)
            )
        }
    }

    @Test
    fun `order tracking steps are ordered sequentially`() {
        assertEquals(6, OrderStatus.trackingSteps.size)
        assertEquals(OrderStatus.PLACED, OrderStatus.trackingSteps.first())
        assertEquals(OrderStatus.DELIVERED, OrderStatus.trackingSteps.last())
    }

    @Test
    fun `product data class includes required firestore fields and no-arg defaults`() {
        val empty = com.example.model.Product()
        assertEquals("", empty.id)
        assertEquals("", empty.name)
        assertEquals(0.0, empty.price, 0.001)

        val product = com.example.model.Product(
            id = "prod_101",
            name = "Ceramide Face Wash",
            description = "Gentle pH-balanced cleanser",
            price = 16.50,
            category = "Face Washes",
            imageUrl = "https://example.com/facewash.jpg"
        )
        val map = product.toWriteMap(isCreate = true)
        assertEquals("Ceramide Face Wash", map["name"])
        assertEquals("Gentle pH-balanced cleanser", map["description"])
        assertEquals(16.50, map["price"])
        assertEquals("Face Washes", map["category"])
        assertEquals("https://example.com/facewash.jpg", map["imageUrl"])
        assertEquals(false, map.containsKey("id"))
    }
}
