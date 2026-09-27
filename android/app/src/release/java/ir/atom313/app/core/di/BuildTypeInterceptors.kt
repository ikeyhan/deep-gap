package ir.atom313.app.core.di

import okhttp3.Interceptor

/** نسخهٔ انتشار: بدون لاگ شبکه. */
object BuildTypeInterceptors {
    fun all(): List<Interceptor> = emptyList()
}
