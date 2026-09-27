package ir.atom313.app.core.network

import ir.atom313.app.core.network.dto.ArticleDto
import ir.atom313.app.core.network.dto.ArticleEnvelope
import ir.atom313.app.core.network.dto.CategoryDto
import ir.atom313.app.core.network.dto.ChangePasswordRequest
import ir.atom313.app.core.network.dto.AuthResponse
import ir.atom313.app.core.network.dto.ChatRequest
import ir.atom313.app.core.network.dto.ChatResponse
import ir.atom313.app.core.network.dto.ClaimOrderRequest
import ir.atom313.app.core.network.dto.ConfigDto
import ir.atom313.app.core.network.dto.DeleteAccountRequest
import ir.atom313.app.core.network.dto.DeviceRequest
import ir.atom313.app.core.network.dto.FaqDto
import ir.atom313.app.core.network.dto.HomeDto
import ir.atom313.app.core.network.dto.ItemsDto
import ir.atom313.app.core.network.dto.MarkReadRequest
import ir.atom313.app.core.network.dto.MeResponse
import ir.atom313.app.core.network.dto.NewTicketRequest
import ir.atom313.app.core.network.dto.NotificationPageDto
import ir.atom313.app.core.network.dto.OfficeDetailDto
import ir.atom313.app.core.network.dto.OfficeDto
import ir.atom313.app.core.network.dto.OfficeMessageRequest
import ir.atom313.app.core.network.dto.OfficeThreadDto
import ir.atom313.app.core.network.dto.OkDto
import ir.atom313.app.core.network.dto.OrderDto
import ir.atom313.app.core.network.dto.OrderEnvelope
import ir.atom313.app.core.network.dto.OrderItemDto
import ir.atom313.app.core.network.dto.PageDto
import ir.atom313.app.core.network.dto.PlaceOrderRequest
import ir.atom313.app.core.network.dto.PlaceOrderResponse
import ir.atom313.app.core.network.dto.ProductDetailDto
import ir.atom313.app.core.network.dto.ProductDto
import ir.atom313.app.core.network.dto.QuoteDto
import ir.atom313.app.core.network.dto.QuoteRequest
import ir.atom313.app.core.network.dto.ReviewRequest
import ir.atom313.app.core.network.dto.SellerDto
import ir.atom313.app.core.network.dto.SellerEnvelope
import ir.atom313.app.core.network.dto.TicketDto
import ir.atom313.app.core.network.dto.TicketEnvelope
import ir.atom313.app.core.network.dto.UpdateProfileRequest
import ir.atom313.app.core.network.dto.UserEnvelope
import ir.atom313.app.core.network.dto.WishlistDto
import ir.atom313.app.core.network.dto.WishlistIdsDto
import ir.atom313.app.core.network.dto.WishlistMergeRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** API اصلی اپ (/api/v1). توکن توسط AuthInterceptor افزوده و در صورت انقضا با TokenAuthenticator نو می‌شود. */
interface AtomApi {
    /* ---------- عمومی ---------- */
    @GET("v1/config") suspend fun config(): ConfigDto
    @GET("v1/home") suspend fun home(): HomeDto
    @GET("v1/categories") suspend fun categories(): ItemsDto<CategoryDto>

    @GET("v1/products")
    suspend fun products(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
        @Query("sellerId") sellerId: Long? = null,
        @Query("minPrice") minPrice: Long? = null,
        @Query("maxPrice") maxPrice: Long? = null,
        @Query("inStock") inStock: String? = null,
        @Query("sort") sort: String? = null,
    ): PageDto<ProductDto>

    @GET("v1/products/{id}") suspend fun product(@Path("id") id: Long): ProductDetailDto
    @POST("v1/products/{id}/reviews") suspend fun addReview(@Path("id") id: Long, @Body body: ReviewRequest): OkDto

    @GET("v1/sellers") suspend fun sellers(@Query("page") page: Int, @Query("limit") limit: Int, @Query("q") q: String? = null): PageDto<SellerDto>
    @GET("v1/sellers/{id}") suspend fun seller(@Path("id") id: Long): SellerEnvelope

    @GET("v1/offices") suspend fun offices(@Query("page") page: Int, @Query("limit") limit: Int, @Query("q") q: String? = null): PageDto<OfficeDto>
    @GET("v1/offices/{id}") suspend fun office(@Path("id") id: Long): OfficeDetailDto
    @POST("v1/offices/{id}/messages") suspend fun messageOffice(@Path("id") id: Long, @Body body: OfficeMessageRequest): OkDto

    @GET("v1/articles") suspend fun articles(@Query("page") page: Int, @Query("limit") limit: Int): PageDto<ArticleDto>
    @GET("v1/articles/{id}") suspend fun article(@Path("id") id: Long): ArticleEnvelope
    @GET("v1/faqs") suspend fun faqs(): ItemsDto<FaqDto>

    @GET("v1/track") suspend fun track(@Query("code") code: String, @Query("phone") phone: String): OrderEnvelope

    /* ---------- سبد و سفارش ---------- */
    @POST("v1/cart/quote") suspend fun quote(@Body body: QuoteRequest): QuoteDto
    @POST("v1/orders") suspend fun placeOrder(@Header("Idempotency-Key") key: String, @Body body: PlaceOrderRequest): PlaceOrderResponse
    @GET("v1/orders") suspend fun orders(@Query("page") page: Int, @Query("limit") limit: Int): PageDto<OrderDto>
    @GET("v1/orders/{code}") suspend fun order(@Path("code") code: String): OrderEnvelope
    @POST("v1/orders/claim") suspend fun claimOrder(@Body body: ClaimOrderRequest): OrderEnvelope
    @GET("v1/my/products") suspend fun myProducts(@Query("page") page: Int, @Query("limit") limit: Int, @Query("status") status: String? = null): PageDto<OrderItemDto>

    /* ---------- حساب ---------- */
    @GET("v1/me") suspend fun me(): MeResponse
    @PUT("v1/me") suspend fun updateMe(@Body body: UpdateProfileRequest): UserEnvelope
    @HTTP(method = "DELETE", path = "v1/me", hasBody = true) suspend fun deleteMe(@Body body: DeleteAccountRequest): OkDto
    @POST("v1/auth/change-password") suspend fun changePassword(@Body body: ChangePasswordRequest): AuthResponse

    @GET("v1/wishlist") suspend fun wishlist(): WishlistDto
    @PUT("v1/wishlist/{id}") suspend fun addWish(@Path("id") productId: Long): OkDto
    @DELETE("v1/wishlist/{id}") suspend fun removeWish(@Path("id") productId: Long): OkDto
    @POST("v1/wishlist/merge") suspend fun mergeWishlist(@Body body: WishlistMergeRequest): WishlistIdsDto

    @GET("v1/notifications") suspend fun notifications(@Query("page") page: Int, @Query("limit") limit: Int, @Query("since") since: Long? = null): NotificationPageDto
    @POST("v1/notifications/read") suspend fun markRead(@Body body: MarkReadRequest): OkDto
    @POST("v1/devices") suspend fun registerDevice(@Body body: DeviceRequest): OkDto

    @GET("v1/support/messages") suspend fun tickets(): ItemsDto<TicketDto>
    @POST("v1/support/messages") suspend fun newTicket(@Body body: NewTicketRequest): TicketEnvelope
    @GET("v1/support/office-messages") suspend fun officeThreads(): ItemsDto<OfficeThreadDto>

    /** گفتگوی هوشمند پشتیبانی (مسیر مشترک با ویجت سایت؛ کلید هوش مصنوعی فقط روی سرور است). */
    @POST("support/chat") suspend fun chat(@Body body: ChatRequest): ChatResponse
}
