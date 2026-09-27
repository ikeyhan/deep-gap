package ir.atom313.app.domain.model

enum class Role { CUSTOMER, SELLER, OFFICE;
    companion object {
        fun from(raw: String) = when (raw) { "seller" -> SELLER; "office" -> OFFICE; else -> CUSTOMER }
    }
}

data class User(
    val id: Long,
    val username: String,
    val name: String,
    val email: String,
    val phone: String,
    val city: String,
    val address: String,
    val role: Role,
    val storeName: String,
    val createdAt: String?,
) {
    val initial: String get() = (name.ifBlank { username }).take(1)
    val firstName: String get() = name.split(' ').firstOrNull().orEmpty().ifBlank { username }
}

data class AccountCounts(val unreadNotifications: Int = 0, val orders: Int = 0, val wishlist: Int = 0)

sealed interface SessionState {
    /** هنوز نشست از حافظهٔ امن خوانده نشده است. */
    data object Unknown : SessionState
    data object Guest : SessionState
    data class SignedIn(val user: User) : SessionState
}
