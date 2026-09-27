package ir.atom313.app

import ir.atom313.app.core.common.ImageUrls
import ir.atom313.app.core.common.Validators
import ir.atom313.app.domain.model.CartQuote
import ir.atom313.app.domain.model.OrderStatus
import ir.atom313.app.domain.model.ProductFilter
import ir.atom313.app.domain.model.ProductSort
import ir.atom313.app.domain.model.QuoteLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test
    fun `persian and arabic digits convert to latin`() {
        assertEquals("09123456789", Validators.latinDigits("۰۹۱۲۳۴۵۶۷۸۹"))
        assertEquals("09123456789", Validators.latinDigits("٠٩١٢٣٤٥٦٧٨٩"))
        assertEquals("abc123", Validators.latinDigits("abc۱۲۳"))
    }

    @Test
    fun `mobile validation accepts persian digits and separators`() {
        assertTrue(Validators.isMobile("۰۹۱۲۳۴۵۶۷۸۹"))
        assertTrue(Validators.isMobile("0912 345 6789"))
        assertFalse(Validators.isMobile("9123456789"))
        assertFalse(Validators.isMobile("08123456789"))
        assertFalse(Validators.isMobile(""))
    }

    @Test
    fun `username rules match the website`() {
        assertTrue(Validators.isUsername("atra"))
        assertTrue(Validators.isUsername("user_1.name"))
        assertFalse(Validators.isUsername("ab"))
        assertFalse(Validators.isUsername("کاربر"))
        assertFalse(Validators.isUsername("user name"))
    }

    @Test
    fun `email validation`() {
        assertTrue(Validators.isEmail("a@b.ir"))
        assertFalse(Validators.isEmail("a@b"))
        assertFalse(Validators.isEmail("ab.ir"))
    }
}

class ImageUrlsTest {
    private val base = "https://atom313.ir/"

    @Test
    fun `relative paths become absolute`() {
        assertEquals("https://atom313.ir/uploads/a.jpg", ImageUrls.resolve(base, "/uploads/a.jpg"))
        assertEquals("https://atom313.ir/assets/img/p.jpg", ImageUrls.resolve(base, "assets/img/p.jpg"))
    }

    @Test
    fun `absolute urls pass through and unsafe schemes are rejected`() {
        assertEquals("https://cdn.example.com/a.jpg", ImageUrls.resolve(base, "https://cdn.example.com/a.jpg"))
        assertNull(ImageUrls.resolve(base, "javascript:alert(1)"))
        assertNull(ImageUrls.resolve(base, "data:image/png;base64,AAA"))
        assertNull(ImageUrls.resolve(base, ""))
        assertNull(ImageUrls.resolve(base, null))
    }
}

class CartQuoteTest {
    private fun quote(
        subtotal: Long = 300_000, discount: Long = 0, shipping: Long = 45_000,
        minOrder: Long = 50_000, freeMin: Long = 500_000, lines: List<QuoteLine> = emptyList(),
    ) = CartQuote(lines, subtotal, discount, shipping, subtotal - discount + shipping, minOrder, freeMin, null, null)

    @Test
    fun `remaining amount to free shipping is computed from the discounted subtotal`() {
        assertEquals(250_000, quote(subtotal = 300_000, discount = 50_000).toFreeShipping)
    }

    @Test
    fun `no remaining amount when shipping is already free`() {
        assertEquals(0, quote(subtotal = 600_000, shipping = 0).toFreeShipping)
    }

    @Test
    fun `below minimum only when the cart is not empty`() {
        assertTrue(quote(subtotal = 20_000).belowMinimum)
        assertFalse(quote(subtotal = 60_000).belowMinimum)
        assertFalse(quote(subtotal = 0).belowMinimum)
    }

    @Test
    fun `problem lines are detected`() {
        val ok = QuoteLine(1, "a", 100, 1, 100, 5, available = true, inStock = true)
        val gone = QuoteLine(2, "b", 100, 1, 100, 0, available = false, inStock = false)
        assertFalse(quote(lines = listOf(ok)).hasProblems)
        assertTrue(quote(lines = listOf(ok, gone)).hasProblems)
    }
}

class OrderStatusTest {
    @Test
    fun `status maps from api values with a safe fallback`() {
        assertEquals(OrderStatus.SHIPPING, OrderStatus.from("shipping"))
        assertEquals(OrderStatus.REVIEW, OrderStatus.from("review"))
        assertEquals(OrderStatus.PENDING, OrderStatus.from("something-new"))
    }

    @Test
    fun `active statuses are the in-progress ones`() {
        assertTrue(OrderStatus.PENDING.isActive)
        assertTrue(OrderStatus.SHIPPING.isActive)
        assertFalse(OrderStatus.DELIVERED.isActive)
        assertFalse(OrderStatus.RETURNED.isActive)
    }
}

class ProductFilterTest {
    @Test
    fun `active filter count drives the filter badge`() {
        assertEquals(0, ProductFilter().activeCount)
        assertEquals(1, ProductFilter(category = "پوشاک").activeCount)
        // price range counts once even with both bounds
        assertEquals(1, ProductFilter(minPrice = 1000, maxPrice = 5000).activeCount)
        assertEquals(3, ProductFilter(category = "x", inStockOnly = true, sort = ProductSort.PRICE_ASC).activeCount)
        // the search text is not a filter chip
        assertEquals(0, ProductFilter(query = "کیف").activeCount)
    }
}
