package ir.atom313.app

import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.CartItemRequest
import ir.atom313.app.core.network.dto.QuoteRequest
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.AppError
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.data.mapper.toMyProduct
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * قرارداد اپ با API نسخهٔ ۱: پاسخ‌های واقعی سرور باید درست به مدل دامنه تبدیل شوند
 * و فیلدهای ناشناخته یا جدید نباید اپ را بشکنند (سازگاری رو به جلو).
 */
class ApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AtomApi

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false; encodeDefaults = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AtomApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun respond(code: Int, body: String) =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))

    @Test
    fun `product detail maps to the domain model`() = runBlocking {
        respond(
            200,
            """{"product":{"id":12,"title":"کیف چرم","category":"پوشاک","seller":"آترا چرم","sellerId":3,
                "price":2450000,"stock":4,"inStock":true,"image":"/uploads/a.jpg","rating":4.7,"reviewCount":9,
                "description":"چرم طبیعی","createdAt":"2026-01-01 10:00:00"},
                "reviews":[{"id":1,"author":"سارا","body":"عالی","rating":5,"createdAt":"2026-01-02 10:00:00"}],
                "ratingDistribution":[0,0,1,2,6],
                "related":[{"id":13,"title":"کمربند","price":680000,"stock":0,"inStock":false}]}""",
        )
        val detail = api.product(12).toDomain()
        assertEquals("کیف چرم", detail.product.title)
        assertEquals(3L, detail.product.sellerId)
        assertEquals(4.7, detail.product.rating!!, 0.001)
        assertEquals(1, detail.reviews.size)
        assertEquals(listOf(0, 0, 1, 2, 6), detail.ratingDistribution)
        assertEquals(1, detail.related.size)
        assertEquals(false, detail.related.first().inStock)
    }

    @Test
    fun `unknown and missing fields do not break parsing`() = runBlocking {
        // سرور آیندهٔ سایت ممکن است فیلد تازه اضافه کند؛ نسخهٔ فعلی اپ باید کار کند
        respond(200, """{"product":{"id":1,"title":"محصول","brandNewField":{"a":1}},"reviews":[],"related":[]}""")
        val detail = api.product(1).toDomain()
        assertEquals("محصول", detail.product.title)
        assertEquals(0L, detail.product.price)
        assertEquals(listOf(0, 0, 0, 0, 0), detail.ratingDistribution) // توزیع غایب → صفر
        assertNull(detail.product.rating)
    }

    @Test
    fun `cart quote sends only product ids and quantities`() = runBlocking {
        respond(200, """{"lines":[],"subtotal":0,"discount":0,"shipping":0,"total":0}""")
        api.quote(QuoteRequest(listOf(CartItemRequest(7, 2)), "ATOM20"))
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertEquals("POST", request.method)
        assertTrue(body.contains("\"productId\":7"))
        assertTrue(body.contains("\"qty\":2"))
        assertTrue(body.contains("\"coupon\":\"ATOM20\""))
        // قیمت هرگز از سمت اپ فرستاده نمی‌شود؛ محاسبه فقط روی سرور است
        assertTrue(!body.contains("\"price\""))
    }

    @Test
    fun `placing an order sends the idempotency key header`() = runBlocking {
        respond(201, """{"order":{"code":"1042","status":"pending","total":100,"itemCount":1,"items":[]},"total":100}""")
        api.placeOrder(
            "and-key-1",
            ir.atom313.app.core.network.dto.PlaceOrderRequest(
                items = listOf(CartItemRequest(1, 1)), name = "علی", phone = "09120000000",
                city = "تهران", address = "خیابان آزادی پلاک ۱",
            ),
        )
        assertEquals("and-key-1", server.takeRequest().getHeader("Idempotency-Key"))
    }

    @Test
    fun `order grouping keeps items and status`() = runBlocking {
        respond(
            200,
            """{"order":{"code":"1042","status":"shipping","createdAt":"2026-02-01 09:00:00","total":3130000,
                "itemCount":2,"note":"ارسال رایگان","recipient":"علی","phone":"09120000000","address":"تهران",
                "items":[{"id":1,"productId":12,"title":"کیف چرم","qty":1,"seller":"آترا","amount":2450000,"status":"shipping"},
                         {"id":2,"productId":13,"title":"کمربند","qty":1,"seller":"آترا","amount":680000,"status":"shipping"}]}}""",
        )
        val order = api.order("1042").order.toDomain()
        assertEquals(ir.atom313.app.domain.model.OrderStatus.SHIPPING, order.status)
        assertEquals(2, order.items.size)
        assertEquals(3130000L, order.total)
        assertEquals("تهران", order.address)
    }

    @Test
    fun `structured server errors surface as user-facing messages`() = runBlocking {
        respond(409, """{"error":{"code":"out_of_stock","message":"موجودی «کیف چرم» کافی نیست."}}""")
        val result = apiCall { api.order("1") }
        assertTrue(result is Outcome.Failure)
        val error = (result as Outcome.Failure).error
        assertEquals("موجودی «کیف چرم» کافی نیست.", error.message)
        assertTrue(error is AppError.Conflict)
    }

    @Test
    fun `config maps commerce and support settings`() = runBlocking {
        respond(
            200,
            """{"site":{"name":"اتم ۳۱۳","phone":"۰۲۱-۹۱۰۰۰۰۰۰","social":{"telegram":"https://t.me/atom"}},
                "commerce":{"shippingCost":45000,"freeShippingMin":500000,"minOrder":50000,"onlinePayment":false,"paymentNote":"هماهنگی تلفنی"},
                "support":{"chatEnabled":true,"aiEnabled":true},"registrationOpen":true,"maintenance":false,
                "android":{"minVersionCode":1,"latestVersionCode":3}}""",
        )
        val config = api.config().toDomain()
        assertEquals(45000L, config.shippingCost)
        assertEquals(false, config.onlinePayment)
        assertEquals("هماهنگی تلفنی", config.paymentNote)
        assertEquals("https://t.me/atom", config.telegram)
        assertTrue(config.aiChatEnabled)
        assertEquals(3, config.latestVersionCode)
    }

    @Test
    fun `my products carry the order code and reorder flag`() = runBlocking {
        respond(
            200,
            """{"items":[{"id":5,"productId":12,"title":"کیف چرم","qty":2,"seller":"آترا","amount":4900000,
                "status":"delivered","orderCode":"1042","purchasedAt":"2026-02-01 09:00:00","canReorder":true}],
                "page":1,"limit":20,"total":1,"hasMore":false}""",
        )
        val page = api.myProducts(1, 20, null)
        val item = page.items.first().toMyProduct()
        assertEquals("1042", item.orderCode)
        assertTrue(item.canReorder)
        assertEquals(2, item.item.qty)
        assertEquals(false, page.hasMore)
    }
}
