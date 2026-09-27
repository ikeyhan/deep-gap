package ir.atom313.app.ui.cart

import ir.atom313.app.data.repository.CartRepository
import ir.atom313.app.domain.model.Product
import javax.inject.Inject

/** افزودن به سبد از هر صفحه‌ای، با پیام یکسان. */
class CartActions @Inject constructor(private val cart: CartRepository) {
    suspend fun add(product: Product, onMessage: (String) -> Unit) {
        if (!product.inStock) { onMessage("این محصول موجود نیست."); return }
        cart.add(product)
        onMessage("«${product.title.take(28)}» به سبد اضافه شد")
    }
}
