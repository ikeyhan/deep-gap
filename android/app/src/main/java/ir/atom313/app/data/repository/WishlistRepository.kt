package ir.atom313.app.data.repository

import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.getOrNull
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.core.di.AppScope
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.WishlistMergeRequest
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * علاقه‌مندی‌ها: برای کاربر واردشده روی سرور (مشترک با سایت)، برای مهمان محلی.
 * پس از ورود، فهرست مهمان با سرور ادغام می‌شود. تغییرات خوش‌بینانه اعمال و در صورت خطا برگردانده می‌شوند.
 */
@Singleton
class WishlistRepository @Inject constructor(
    private val api: AtomApi,
    private val prefs: UserPreferences,
    private val session: SessionRepository,
    @AppScope scope: CoroutineScope,
) {
    private val serverIds = MutableStateFlow<Set<Long>>(emptySet())

    val ids: StateFlow<Set<Long>> = combine(session.state, serverIds, prefs.guestWishlist) { s, server, guest ->
        if (s is SessionState.SignedIn) server else guest
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    /** تغییر وضعیت؛ نتیجه: true = اکنون در فهرست است. */
    suspend fun toggle(productId: Long): Outcome<Boolean> {
        val adding = productId !in ids.value
        if (!session.isSignedIn) {
            val cur = prefs.guestWishlist.first()
            prefs.setGuestWishlist(if (adding) cur + productId else cur - productId)
            return Outcome.Success(adding)
        }
        val before = serverIds.value
        serverIds.value = if (adding) before + productId else before - productId
        val r = apiCall { if (adding) api.addWish(productId) else api.removeWish(productId) }
        return when (r) {
            is Outcome.Success -> Outcome.Success(adding)
            is Outcome.Failure -> { serverIds.value = before; r }
        }
    }

    /** همگام‌سازی با سرور (ورود، بازگشت به اپ). فهرست مهمان ادغام و پاک می‌شود. */
    suspend fun sync(): Outcome<Unit> {
        if (!session.isSignedIn) return Outcome.Success(Unit)
        val guest = prefs.guestWishlist.first()
        val r: Outcome<List<Long>> = if (guest.isNotEmpty()) {
            apiCall { api.mergeWishlist(WishlistMergeRequest(guest.toList())).ids }
        } else {
            apiCall { api.wishlist().ids }
        }
        return when (r) {
            is Outcome.Success -> {
                if (guest.isNotEmpty()) prefs.setGuestWishlist(emptySet())
                serverIds.value = r.value.toSet()
                Outcome.Success(Unit)
            }
            is Outcome.Failure -> r
        }
    }

    fun clearServerState() { serverIds.value = emptySet() }

    /** محصولات فهرست برای صفحهٔ علاقه‌مندی‌ها. */
    fun items(): Flow<LoadState<List<Product>>> = flow {
        emit(LoadState.loading())
        if (session.isSignedIn) {
            when (val r = apiCall { api.wishlist() }) {
                is Outcome.Success -> {
                    serverIds.value = r.value.ids.toSet()
                    emit(LoadState(r.value.items.map { it.toDomain() }))
                }
                is Outcome.Failure -> emit(LoadState(error = r.error))
            }
        } else {
            // مهمان: جزئیات هر محصول به‌صورت موازی (فهرست محلی کوچک است)
            val list = coroutineScope {
                prefs.guestWishlist.first().take(50).map { id -> async { apiCall { api.product(id).product }.getOrNull() } }.awaitAll()
            }.filterNotNull().map { it.toDomain() }
            emit(LoadState(list))
        }
    }
}
