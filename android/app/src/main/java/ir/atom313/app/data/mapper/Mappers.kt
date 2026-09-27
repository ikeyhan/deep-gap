package ir.atom313.app.data.mapper

import ir.atom313.app.core.network.dto.AnnouncementDto
import ir.atom313.app.core.network.dto.ArticleDto
import ir.atom313.app.core.network.dto.CategoryDto
import ir.atom313.app.core.network.dto.ConfigDto
import ir.atom313.app.core.network.dto.CountsDto
import ir.atom313.app.core.network.dto.FaqDto
import ir.atom313.app.core.network.dto.HomeDto
import ir.atom313.app.core.network.dto.NotificationDto
import ir.atom313.app.core.network.dto.OfficeDetailDto
import ir.atom313.app.core.network.dto.OfficeDto
import ir.atom313.app.core.network.dto.OfficeThreadDto
import ir.atom313.app.core.network.dto.OrderDto
import ir.atom313.app.core.network.dto.OrderItemDto
import ir.atom313.app.core.network.dto.PlaceOrderResponse
import ir.atom313.app.core.network.dto.ProductDetailDto
import ir.atom313.app.core.network.dto.ProductDto
import ir.atom313.app.core.network.dto.QuoteDto
import ir.atom313.app.core.network.dto.ReviewDto
import ir.atom313.app.core.network.dto.SellerDto
import ir.atom313.app.core.network.dto.SlideDto
import ir.atom313.app.core.network.dto.TicketDto
import ir.atom313.app.core.network.dto.UserDto
import ir.atom313.app.domain.model.AccountCounts
import ir.atom313.app.domain.model.Announcement
import ir.atom313.app.domain.model.AppConfig
import ir.atom313.app.domain.model.AppNotification
import ir.atom313.app.domain.model.Article
import ir.atom313.app.domain.model.CartQuote
import ir.atom313.app.domain.model.Category
import ir.atom313.app.domain.model.Coupon
import ir.atom313.app.domain.model.Faq
import ir.atom313.app.domain.model.Home
import ir.atom313.app.domain.model.MyProduct
import ir.atom313.app.domain.model.NotificationType
import ir.atom313.app.domain.model.Office
import ir.atom313.app.domain.model.OfficeDetail
import ir.atom313.app.domain.model.OfficeService
import ir.atom313.app.domain.model.OfficeThread
import ir.atom313.app.domain.model.Order
import ir.atom313.app.domain.model.OrderItem
import ir.atom313.app.domain.model.OrderStatus
import ir.atom313.app.domain.model.PlacedOrder
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.ProductDetail
import ir.atom313.app.domain.model.QuoteLine
import ir.atom313.app.domain.model.Review
import ir.atom313.app.domain.model.Role
import ir.atom313.app.domain.model.Seller
import ir.atom313.app.domain.model.Slide
import ir.atom313.app.domain.model.Ticket
import ir.atom313.app.domain.model.TicketStatus
import ir.atom313.app.domain.model.User

fun UserDto.toDomain() = User(id, username, name.ifBlank { username }, email, phone, city, address, Role.from(role), storeName, createdAt)
fun CountsDto.toDomain() = AccountCounts(unreadNotifications, orders, wishlist)

fun ProductDto.toDomain() = Product(
    id = id, title = title, category = category, seller = seller, sellerId = sellerId, price = price,
    stock = stock, inStock = inStock, image = image, rating = rating, reviewCount = reviewCount,
    description = description.orEmpty(), available = available,
)
fun ReviewDto.toDomain() = Review(id, author, body, rating, createdAt)
fun ProductDetailDto.toDomain() = ProductDetail(
    product = product.toDomain(),
    reviews = reviews.map { it.toDomain() },
    ratingDistribution = ratingDistribution.takeIf { it.size == 5 } ?: List(5) { 0 },
    related = related.map { it.toDomain() },
)
fun CategoryDto.toDomain() = Category(name, count)
fun SlideDto.toDomain() = Slide(id, eyebrow, title, subtitle, image, ctaLabel, ctaLink)
fun AnnouncementDto.toDomain() = Announcement(title, text, link, ctaLabel)
fun SellerDto.toDomain() = Seller(id, name, category, city, rating, sales, avatar, bio.orEmpty(), createdAt, productCount)
fun OfficeDto.toDomain() = Office(id, name, manager, area, city, avatar, rating, address, phone, bio)
fun OfficeDetailDto.toDomain() = OfficeDetail(office.toDomain(), services.map { OfficeService(it.id, it.title, it.category, it.description, it.price) })
fun ArticleDto.toDomain() = Article(id, title, category, author, excerpt, cover, views, createdAt, body)
fun FaqDto.toDomain() = Faq(id, question, answer)
fun HomeDto.toDomain() = Home(
    slides = slides.map { it.toDomain() },
    announcement = announcement?.takeIf { it.title.isNotBlank() || it.text.isNotBlank() }?.toDomain(),
    categories = categories.map { it.toDomain() },
    newest = newest.map { it.toDomain() },
    popular = popular.map { it.toDomain() },
    sellers = sellers.map { it.toDomain() },
    offices = offices.map { it.toDomain() },
    articles = articles.map { it.toDomain() },
)

fun OrderItemDto.toItem() = OrderItem(id, productId, title, qty, seller, amount, OrderStatus.from(status), image)
fun OrderItemDto.toMyProduct() = MyProduct(toItem(), orderCode.orEmpty(), purchasedAt, canReorder)
fun OrderDto.toDomain() = Order(
    code = code, status = OrderStatus.from(status), createdAt = createdAt, total = total, itemCount = itemCount,
    items = items.map { it.toItem() }, note = note, recipient = recipient, phone = phone, address = address,
)
fun PlaceOrderResponse.toDomain() = PlacedOrder(order.toDomain(), subtotal, discount, shipping, total)

fun QuoteDto.toDomain() = CartQuote(
    lines = lines.map { QuoteLine(it.productId, it.title, it.price, it.qty, it.amount, it.stock, it.available, it.inStock) },
    subtotal = subtotal, discount = discount, shipping = shipping, total = total,
    minOrder = minOrder, freeShippingMin = freeShippingMin,
    coupon = coupon?.let { Coupon(it.code, it.title, it.kind, it.amount) }, couponError = couponError,
)

fun NotificationDto.toDomain() = AppNotification(id, NotificationType.from(type), title, body, ref, read, createdAt)
fun TicketDto.toDomain() = Ticket(id, subject, body, reply, TicketStatus.from(status), createdAt)
fun OfficeThreadDto.toDomain() = OfficeThread(id, office, body, reply, reply.isNotBlank(), createdAt)

fun ConfigDto.toDomain() = AppConfig(
    siteName = site.name.ifBlank { "اتم ۳۱۳" }, siteDescription = site.description, domain = site.domain,
    email = site.email, phone = site.phone, address = site.address,
    instagram = site.social.instagram, telegram = site.social.telegram, whatsapp = site.social.whatsapp,
    shippingCost = commerce.shippingCost, freeShippingMin = commerce.freeShippingMin, minOrder = commerce.minOrder,
    onlinePayment = commerce.onlinePayment,
    paymentNote = commerce.paymentNote.ifBlank { AppConfig().paymentNote },
    chatEnabled = support.chatEnabled, aiChatEnabled = support.aiEnabled, chatWelcome = support.welcome,
    registrationOpen = registrationOpen, maintenance = maintenance,
    minVersionCode = android.minVersionCode, latestVersionCode = android.latestVersionCode,
)
