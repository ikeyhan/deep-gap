package ir.atom313.app.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** رویدادهای سراسری لایهٔ شبکه که UI به آن‌ها واکنش نشان می‌دهد. */
@Singleton
class SessionEvents @Inject constructor() {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** نشست باطل شد (refresh رد شد) → کاربر باید دوباره وارد شود. */
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()
    fun notifyExpired() { _expired.tryEmit(Unit) }

    private val _maintenance = MutableStateFlow(false)
    /** سرور در حالت تعمیرات است. */
    val maintenance: StateFlow<Boolean> = _maintenance.asStateFlow()
    fun setMaintenance(on: Boolean) { if (_maintenance.value != on) _maintenance.value = on }
}
