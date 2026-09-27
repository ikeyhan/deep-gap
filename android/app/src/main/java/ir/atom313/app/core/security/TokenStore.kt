package ir.atom313.app.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.atom313.app.core.network.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
)

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * نگه‌داری امن نشست: کل نشست (توکن‌ها + پروفایل) یک‌جا با کلید Keystore رمز می‌شود.
 * نسخهٔ رمزگشایی‌شده فقط در حافظه نگه داشته می‌شود تا Interceptor بدون I/O به آن دسترسی داشته باشد.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cipher: KeystoreCipher,
    private val json: Json,
) {
    private val key = stringPreferencesKey("blob")
    private val mutex = Mutex()
    private val _session = MutableStateFlow<StoredSession?>(null)
    val session: StateFlow<StoredSession?> = _session.asStateFlow()

    @Volatile private var loaded = false

    /** یک‌بار هنگام شروع (پیش از اولین درخواست) صدا زده می‌شود. */
    suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val blob = context.sessionStore.data.first()[key]
            _session.value = blob?.let { cipher.decrypt(it) }?.let {
                runCatching { json.decodeFromString<StoredSession>(it) }.getOrNull()
            }
            if (blob != null && _session.value == null) context.sessionStore.edit { it.clear() }
            loaded = true
        }
    }

    val accessToken: String? get() = _session.value?.accessToken
    val refreshToken: String? get() = _session.value?.refreshToken

    suspend fun save(session: StoredSession) = mutex.withLock {
        val blob = cipher.encrypt(json.encodeToString(session))
        context.sessionStore.edit { it[key] = blob }
        _session.value = session
    }

    suspend fun updateUser(user: UserDto) {
        val cur = _session.value ?: return
        save(cur.copy(user = user))
    }

    suspend fun clear() = mutex.withLock {
        context.sessionStore.edit { it.clear() }
        _session.value = null
    }
}
