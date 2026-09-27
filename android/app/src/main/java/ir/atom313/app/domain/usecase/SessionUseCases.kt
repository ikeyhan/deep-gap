package ir.atom313.app.domain.usecase

import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.core.di.AppScope
import ir.atom313.app.data.repository.NotificationRepository
import ir.atom313.app.data.repository.OrderRepository
import ir.atom313.app.data.repository.RegisterInput
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.data.repository.WishlistRepository
import ir.atom313.app.domain.model.User
import ir.atom313.app.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * کارهای پس از ورود/ثبت‌نام، در پس‌زمینه و بدون مسدود کردن UI:
 * ادغام علاقه‌مندی مهمان، اتصال آخرین سفارش مهمان به حساب، ثبت دستگاه و زمان‌بندی اعلان‌ها.
 */
class PostSignInSync @Inject constructor(
    private val session: SessionRepository,
    private val wishlist: WishlistRepository,
    private val orders: OrderRepository,
    private val notifications: NotificationRepository,
    private val prefs: UserPreferences,
    private val scheduler: NotificationScheduler,
    @AppScope private val scope: CoroutineScope,
) {
    operator fun invoke() {
        scope.launch {
            launch { wishlist.sync() }
            launch { session.refreshMe() }
            launch {
                prefs.guestOrder.first()?.let { g ->
                    if (orders.claim(g.code, g.phone) is Outcome.Success) prefs.setGuestOrder(null)
                }
            }
            launch { notifications.registerDevice() }
            scheduler.schedule()
        }
    }
}

class LoginUseCase @Inject constructor(
    private val session: SessionRepository,
    private val sync: PostSignInSync,
) {
    suspend operator fun invoke(username: String, password: String): Outcome<User> {
        val fields = buildMap {
            if (username.isBlank()) put("username", "نام کاربری یا موبایل را وارد کنید.")
            if (password.isEmpty()) put("password", "رمز عبور را وارد کنید.")
        }
        if (fields.isNotEmpty()) return Outcome.Failure(AppError.Validation("invalid_input", "اطلاعات ورود را کامل کنید.", fields))
        return session.login(username, password).also { if (it is Outcome.Success) sync() }
    }
}

class RegisterUseCase @Inject constructor(
    private val session: SessionRepository,
    private val sync: PostSignInSync,
) {
    suspend operator fun invoke(input: RegisterInput): Outcome<User> {
        val fields = buildMap {
            if (input.name.trim().length < 2) put("name", "نام و نام خانوادگی را وارد کنید.")
            if (!Validators.isMobile(input.phone)) put("phone", "شمارهٔ موبایل نامعتبر است (نمونه: ۰۹۱۲۳۴۵۶۷۸۹).")
            if (!Validators.isUsername(input.username)) put("username", "فقط حروف و اعداد انگلیسی، حداقل ۳ نویسه.")
            if (input.password.length < Validators.MIN_PASSWORD) put("password", "رمز عبور باید حداقل ۸ نویسه باشد.")
            if (input.email.isNotBlank() && !Validators.isEmail(input.email)) put("email", "ایمیل نامعتبر است.")
        }
        if (fields.isNotEmpty()) return Outcome.Failure(AppError.Validation("invalid_input", "لطفاً خطاهای فرم را برطرف کنید.", fields))
        return session.register(input).also { if (it is Outcome.Success) sync() }
    }
}

class LogoutUseCase @Inject constructor(
    private val session: SessionRepository,
    private val wishlist: WishlistRepository,
    private val notifications: NotificationRepository,
    private val scheduler: NotificationScheduler,
) {
    suspend operator fun invoke() {
        scheduler.cancel()
        session.logout(notifications.deviceToken())
        wishlist.clearServerState()
    }
}
