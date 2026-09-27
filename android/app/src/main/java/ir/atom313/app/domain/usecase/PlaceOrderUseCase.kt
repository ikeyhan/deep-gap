package ir.atom313.app.domain.usecase

import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.core.datastore.GuestOrder
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.data.repository.CartRepository
import ir.atom313.app.data.repository.OrderRepository
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.domain.model.CheckoutForm
import ir.atom313.app.domain.model.PlacedOrder
import javax.inject.Inject

/**
 * ثبت نهایی سفارش. مبلغ را سرور محاسبه و تأیید می‌کند؛ اپ فقط اقلام و اطلاعات ارسال را می‌فرستد.
 * کلید یکتا ([idempotencyKey]) تضمین می‌کند تکرار درخواست (قطع شبکه، لمس دوباره) سفارش تکراری نسازد.
 */
class PlaceOrderUseCase @Inject constructor(
    private val cart: CartRepository,
    private val orders: OrderRepository,
    private val session: SessionRepository,
    private val prefs: UserPreferences,
) {
    fun validate(form: CheckoutForm): Map<String, String> = buildMap {
        if (form.name.trim().length < 2) put("name", "نام گیرنده را وارد کنید.")
        if (!Validators.isMobile(form.phone)) put("phone", "شمارهٔ موبایل نامعتبر است (نمونه: ۰۹۱۲۳۴۵۶۷۸۹).")
        if (form.address.trim().length < 10) put("address", "آدرس کامل پستی را وارد کنید.")
    }

    suspend operator fun invoke(form: CheckoutForm, coupon: String?, idempotencyKey: String): Outcome<PlacedOrder> {
        val fields = validate(form)
        if (fields.isNotEmpty()) return Outcome.Failure(AppError.Validation("invalid_input", "لطفاً اطلاعات ارسال را کامل کنید.", fields))
        val lines = cart.snapshot()
        if (lines.isEmpty()) return Outcome.Failure(AppError.Validation("cart_empty", "سبد خرید خالی است."))
        val r = orders.place(form, lines, coupon, idempotencyKey)
        if (r is Outcome.Success) {
            cart.clear()
            // مهمان: برای پیگیری و اتصال خودکار سفارش به حساب پس از ورود
            if (!session.isSignedIn) prefs.setGuestOrder(GuestOrder(r.value.order.code, Validators.normalizeMobile(form.phone)))
        }
        return r
    }
}
